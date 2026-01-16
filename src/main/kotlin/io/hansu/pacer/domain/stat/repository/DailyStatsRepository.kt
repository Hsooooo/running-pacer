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
            on duplicate key update
              run_count = run_count + 1,
              total_distance_m = total_distance_m + values(total_distance_m),
              total_moving_time_s = total_moving_time_s + values(total_moving_time_s),
              avg_pace_sec_per_km = 
                case
                  when values(avg_pace_sec_per_km) is null then avg_pace_sec_per_km
                  when avg_pace_sec_per_km is null then values(avg_pace_sec_per_km)
                  else floor(
                    (
                      (avg_pace_sec_per_km * total_moving_time_s) +
                      (values(avg_pace_sec_per_km) * values(total_moving_time_s))
                    ) / (total_moving_time_s + values(total_moving_time_s))
                  )
                end,
              avg_hr =
                case
                  when values(avg_hr) is null then avg_hr
                  when avg_hr is null then values(avg_hr)
                  else floor(
                    (
                      (avg_hr * total_moving_time_s) +
                      (values(avg_hr) * values(total_moving_time_s))
                    ) / (total_moving_time_s + values(total_moving_time_s))
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
