package io.hansu.pacer.domain.stat.repository

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class DailyStatsRepository(
    private val jdbc: NamedParameterJdbcTemplate
) {
    fun upsertDaily(
        userId: Long,
        statDate: LocalDate,
        addDistanceM: Int,
        addMovingTimeS: Int,
        addAvgPaceSecPerKm: Int?,
        addAvgHr: Int?
    ) {
        val sql = """
            insert into daily_stats
              (user_id, stat_date, run_count, total_distance_m, total_moving_time_s, avg_pace_sec_per_km, avg_hr)
            values
              (:userId, :statDate, 1, :addDistanceM, :addMovingTimeS, :addAvgPace, :addAvgHr)
            on conflict (user_id, stat_date) do update set
              run_count = daily_stats.run_count + 1,
              total_distance_m = daily_stats.total_distance_m + excluded.total_distance_m,
              total_moving_time_s = daily_stats.total_moving_time_s + excluded.total_moving_time_s,
              avg_pace_sec_per_km = 
                case
                  when excluded.avg_pace_sec_per_km is null then daily_stats.avg_pace_sec_per_km
                  when daily_stats.avg_pace_sec_per_km is null then excluded.avg_pace_sec_per_km
                  else floor(
                    (
                      (daily_stats.avg_pace_sec_per_km * daily_stats.total_moving_time_s) +
                      (excluded.avg_pace_sec_per_km * excluded.total_moving_time_s)
                    ) / (daily_stats.total_moving_time_s + excluded.total_moving_time_s)
                  )
                end,
              avg_hr =
                case
                  when excluded.avg_hr is null then daily_stats.avg_hr
                  when daily_stats.avg_hr is null then excluded.avg_hr
                  else floor(
                    (
                      (daily_stats.avg_hr * daily_stats.total_moving_time_s) +
                      (excluded.avg_hr * excluded.total_moving_time_s)
                    ) / (daily_stats.total_moving_time_s + excluded.total_moving_time_s)
                  )
                end,
              updated_at = current_timestamp
        """.trimIndent()

        val params = mapOf(
            "userId" to userId,
            "statDate" to statDate,
            "addDistanceM" to addDistanceM,
            "addMovingTimeS" to addMovingTimeS,
            "addAvgPace" to (addAvgPaceSecPerKm ?: 0),
            "addAvgHr" to addAvgHr
        )
        jdbc.update(sql, params)
    }
}
