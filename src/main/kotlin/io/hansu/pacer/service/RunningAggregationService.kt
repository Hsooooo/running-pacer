package io.hansu.pacer.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.hansu.pacer.domain.activity.repository.ActivityRepository
import io.hansu.pacer.domain.activity.repository.ActivityStreamsRepository
import io.hansu.pacer.domain.stat.repository.DailyStatsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.*

@Service
class RunningAggregationService(
    private val activityRepo: ActivityRepository,
    private val streamsRepo: ActivityStreamsRepository,
    private val dailyStatsRepo: DailyStatsRepository
) {
    private val om = jacksonObjectMapper()

    @Transactional
    fun refreshActivityDerivedAndDailyStats(userId: Long, activityId: Long) {
        val activity = activityRepo.findByIdOrThrow(activityId)

        // 1) avg pace 계산(스트림 우선)
        val paceFromStream = streamsRepo.findByIdOrNull(activityId)?.let { streams ->
            val root = om.readTree(streams.streamsJson)
            val velocity = root.path("velocity_smooth").path("data")
            if (velocity.isArray && velocity.size() > 0) {
                val valid = velocity.mapNotNull { v ->
                    val dv = v.asDouble()
                    if (dv > 0.3) dv else null // 정지/노이즈 컷
                }
                if (valid.isNotEmpty()) {
                    val avgV = valid.average()
                    (1000.0 / avgV).toInt() // sec/km
                } else null
            } else null
        }

        val avgPace = paceFromStream ?: run {
            if (activity.movingTimeS > 0 && activity.distanceM > 0) {
                (activity.movingTimeS.toDouble() / (activity.distanceM / 1000.0)).toInt()
            } else null
        }

        // 2) activities 업데이트 (avg_pace_sec_per_km)
        if (avgPace != null) {
            activityRepo.updateAvgPace(activityId, avgPace)
        }

        // 3) daily_stats upsert
        val statDate = activity.startTimeUtc.toLocalDate() // UTC 기준. 필요하면 user timezone 반영(Phase 1은 단순화)
        dailyStatsRepo.upsertDaily(
            userId = userId,
            statDate = statDate,
            addDistanceM = activity.distanceM,
            addMovingTimeS = activity.movingTimeS,
            addAvgPaceSecPerKm = avgPace,
            addAvgHr = activity.avgHr
        )
    }
}
