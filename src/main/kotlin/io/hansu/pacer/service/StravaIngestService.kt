package io.hansu.pacer.service
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.hansu.pacer.domain.activity.ActivityStreamsEntity
import io.hansu.pacer.domain.activity.LapEntity
import io.hansu.pacer.domain.activity.repository.ActivityRepository
import io.hansu.pacer.domain.activity.repository.ActivityStreamsRepository
import io.hansu.pacer.domain.activity.repository.LapRepository
import io.hansu.pacer.domain.raw.repository.RawStravaPayloadRepository
import io.hansu.pacer.domain.stat.repository.DailyStatsRepository
import io.hansu.pacer.util.KmaWeatherClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.*

@Service
class StravaIngestService(
    private val activityRepo: ActivityRepository,
    private val lapRepo: LapRepository,
    private val streamsRepo: ActivityStreamsRepository,
    private val dailyStatsRepo: DailyStatsRepository,
    private val rawRepo: RawStravaPayloadRepository,
    private val aggService: RunningAggregationService,
    private val weatherClient: KmaWeatherClient
) {
    private val om = jacksonObjectMapper()

    @Transactional
    fun ingest(
        userId: Long,
        activityJson: JsonNode,
        lapsJson: JsonNode?,      // 없을 수 있음
        streamsJson: JsonNode?    // 없을 수 있음
    ): Long {
        val sourceActivityId = activityJson["id"].asLong()

        // 1) raw 저장(디버그/리플레이)
        rawRepo.saveRaw(userId, "activity", sourceActivityId, activityJson.toString())
        if (streamsJson != null) rawRepo.saveRaw(userId, "streams", sourceActivityId, streamsJson.toString())

        // 2) activities upsert
        val startTimeUtc = OffsetDateTime.parse(activityJson["start_date"].asText()).toLocalDateTime() // Strava는 UTC start_date
        val distanceM = activityJson["distance"].asDouble().toInt()
        val movingTimeS = activityJson["moving_time"].asInt()
        val elapsedTimeS = activityJson["elapsed_time"].asInt()

        val avgHr = activityJson.path("average_heartrate").takeIf { !it.isMissingNode && !it.isNull }?.asDouble()?.toInt()
        val maxHr = activityJson.path("max_heartrate").takeIf { !it.isMissingNode && !it.isNull }?.asDouble()?.toInt()

        val activity = activityRepo.upsertBySourceId(
            userId = userId,
            source = "STRAVA",
            sourceActivityId = sourceActivityId,
            startTimeUtc = startTimeUtc,
            timezone = activityJson.path("timezone").takeIf { !it.isMissingNode && !it.isNull }?.asText(),
            distanceM = distanceM,
            movingTimeS = movingTimeS,
            elapsedTimeS = elapsedTimeS,
            avgHr = avgHr,
            maxHr = maxHr,
            elevationGainM = activityJson.path("total_elevation_gain").takeIf { !it.isMissingNode && !it.isNull }?.asDouble()?.toInt()
        )

        // 3) laps upsert
        if (lapsJson != null && lapsJson.isArray) {
            val laps = lapsJson.mapIndexed { idx, lap ->
                val lapDistanceM = lap["distance"].asDouble().toInt()
                val lapMovingS = lap["moving_time"].asInt()
                LapEntity(
                    activityId = activity.id,
                    lapIndex = idx,
                    distanceM = lapDistanceM,
                    movingTimeS = lapMovingS,
                    avgHr = lap.path("average_heartrate").takeIf { !it.isMissingNode && !it.isNull }?.asDouble()
                        ?.toInt(),
                    avgCadence = lap.path("average_cadence").takeIf { !it.isMissingNode && !it.isNull }?.asDouble()
                        ?.toInt(),
                    elevationGainM = lap.path("total_elevation_gain").takeIf { !it.isMissingNode && !it.isNull }
                        ?.asDouble()?.toInt()
                )
            }
            lapRepo.upsertAll(activity.id, laps)
        }

        // 4) streams 저장(통 저장)
        if (streamsJson != null) {
            val keys = streamsJson.fieldNames().asSequence().toList().sorted()
            val sampleCount = streamsJson.path("time")?.path("data")?.size() ?: null
            streamsRepo.upsert(
                ActivityStreamsEntity(
                    activityId = activity.id,
                    userId = userId,
                    streamsJson = streamsJson.toString(),
                    streamKeys = keys.joinToString(","),
                    sampleCount = sampleCount
                )
            )
        }

        // 4.5) Fetch and save weather data if available
        try {
            weatherClient.getUltraSrtForecast()?.let { weather ->
                activityRepo.updateWeather(
                    activityId = activity.id,
                    temp = weather.temp,
                    humidity = weather.humidity,
                    windSpeed = weather.windSpeed,
                    precipType = weather.precipType,
                    sky = weather.sky
                )
            }
        } catch (e: Exception) {
            // Weather data is optional, don't fail the ingest
            println("Warning: Failed to fetch weather data: ${e.message}")
        }

        // 5) 파생값/집계 생성 (핵심)
        aggService.refreshActivityDerivedAndDailyStats(userId, activity.id)

        return activity.id
    }
}
