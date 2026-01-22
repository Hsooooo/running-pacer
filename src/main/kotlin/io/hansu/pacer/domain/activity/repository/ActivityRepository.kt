package io.hansu.pacer.domain.activity.repository

import io.hansu.pacer.domain.activity.ActivityEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

interface ActivityJpaRepository : JpaRepository<ActivityEntity, Long> {

    fun findBySourceAndSourceActivityId(source: String, sourceActivityId: Long): ActivityEntity?

    @Modifying
    @Query("update ActivityEntity a set a.avgPaceSecPerKm = :avgPace where a.id = :id")
    fun updateAvgPace(@Param("id") id: Long, @Param("avgPace") avgPace: Int): Int
}

@Repository
class ActivityRepository(
    private val jpa: ActivityJpaRepository,
    private val jdbc: NamedParameterJdbcTemplate
) {
    fun findByIdOrThrow(id: Long): ActivityEntity =
        jpa.findById(id).orElseThrow { IllegalArgumentException("activity not found: $id") }

    @Transactional
    fun upsertBySourceId(
        userId: Long,
        source: String,
        sourceActivityId: Long,
        startTimeUtc: LocalDateTime,
        timezone: String?,
        distanceM: Int,
        movingTimeS: Int,
        elapsedTimeS: Int,
        avgHr: Int?,
        maxHr: Int?,
        elevationGainM: Int?
    ): ActivityEntity {

        // 1) upsert (유니크키: source + source_activity_id)
        val sql = """
            insert into activities
            (user_id, source, source_activity_id, sport_type, start_time_utc, timezone,
             distance_m, moving_time_s, elapsed_time_s, avg_hr, max_hr, elevation_gain_m)
            values
            (:userId, :source, :sourceActivityId, 'RUN', :startTimeUtc, :timezone,
             :distanceM, :movingTimeS, :elapsedTimeS, :avgHr, :maxHr, :elevationGainM)
            on conflict (source, source_activity_id) do update set
              user_id = excluded.user_id,
              start_time_utc = excluded.start_time_utc,
              timezone = excluded.timezone,
              distance_m = excluded.distance_m,
              moving_time_s = excluded.moving_time_s,
              elapsed_time_s = excluded.elapsed_time_s,
              avg_hr = excluded.avg_hr,
              max_hr = excluded.max_hr,
              elevation_gain_m = excluded.elevation_gain_m,
              updated_at = current_timestamp
        """.trimIndent()

        val params = mapOf(
            "userId" to userId,
            "source" to source,
            "sourceActivityId" to sourceActivityId,
            "startTimeUtc" to startTimeUtc,
            "timezone" to timezone,
            "distanceM" to distanceM,
            "movingTimeS" to movingTimeS,
            "elapsedTimeS" to elapsedTimeS,
            "avgHr" to avgHr,
            "maxHr" to maxHr,
            "elevationGainM" to elevationGainM
        )
        jdbc.update(sql, params)

        // 2) 다시 조회해서 entity 반환
        return jpa.findBySourceAndSourceActivityId(source, sourceActivityId)
            ?: throw IllegalStateException("upsert failed for activity source=$source id=$sourceActivityId")
    }

    @Transactional
    fun updateAvgPace(activityId: Long, avgPace: Int) {
        jpa.updateAvgPace(activityId, avgPace)
    }

    @Transactional
    fun updateWeather(activityId: Long, temp: Int?, humidity: Int?, windSpeed: Int?, precipType: String?, sky: String?) {
        val sql = """
            update activities set
                weather_temp = :temp,
                weather_humidity = :humidity,
                weather_wind_speed = :windSpeed,
                weather_precip_type = :precipType,
                weather_sky = :sky,
                updated_at = current_timestamp
            where activity_id = :activityId
        """.trimIndent()
        jdbc.update(sql, mapOf(
            "activityId" to activityId,
            "temp" to temp,
            "humidity" to humidity,
            "windSpeed" to windSpeed,
            "precipType" to precipType,
            "sky" to sky
        ))
    }
}