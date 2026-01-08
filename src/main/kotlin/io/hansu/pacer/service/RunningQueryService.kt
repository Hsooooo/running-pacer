package io.hansu.pacer.service

import io.hansu.pacer.dto.ActivitySummary
import io.hansu.pacer.dto.AnomalyRun
import io.hansu.pacer.dto.AnomalyType
import io.hansu.pacer.dto.PeriodComparison
import io.hansu.pacer.dto.PeriodSummary
import io.hansu.pacer.dto.TrendMetric
import io.hansu.pacer.dto.TrendPoint
import io.hansu.pacer.service.RunningQueryService.Companion.DEFAULT_USER_ID
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import java.sql.ResultSet
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class RunningQueryService(
    private val jdbc: NamedParameterJdbcTemplate
) {

    fun listActivities(from: LocalDate, to: LocalDate, limit: Int = 30): List<ActivitySummary> {
        // [from, to] inclusive
        val sql = """
            select
              a.activity_id,
              a.start_time_utc,
              a.distance_m,
              a.moving_time_s,
              a.avg_pace_sec_per_km,
              a.avg_hr
            from activities a
            where a.user_id = :userId
              and a.sport_type = 'RUN'
              and a.start_time_utc >= :fromDt
              and a.start_time_utc < :toDtExclusive
            order by a.start_time_utc desc
            limit :limit
        """.trimIndent()

        val params = mapOf(
            "userId" to DEFAULT_USER_ID,
            "fromDt" to from.atStartOfDay(),
            "toDtExclusive" to to.plusDays(1).atStartOfDay(),
            "limit" to limit
        )

        return jdbc.query(sql, params) { rs, _ -> rs.toActivitySummary() }
    }

    fun getPeriodSummary(from: LocalDate, to: LocalDate): PeriodSummary {
        // daily_stats는 이미 증분 집계되어 있으므로 이걸 우선 사용
        val sql = """
            select
              coalesce(sum(run_count), 0) as run_count,
              coalesce(sum(total_distance_m), 0) as total_distance_m,
              coalesce(sum(total_moving_time_s), 0) as total_moving_time_s,
              -- avg는 "가중평균"으로 다시 계산(시간 가중)
              case
                when coalesce(sum(total_moving_time_s), 0) = 0 then null
                else floor(sum(coalesce(avg_pace_sec_per_km, 0) * total_moving_time_s) / sum(total_moving_time_s))
              end as avg_pace_sec_per_km,
              case
                when coalesce(sum(total_moving_time_s), 0) = 0 then null
                else floor(sum(coalesce(avg_hr, 0) * total_moving_time_s) / sum(total_moving_time_s))
              end as avg_hr
            from daily_stats
            where user_id = :userId
              and stat_date >= :fromDate
              and stat_date <= :toDate
        """.trimIndent()

        val params = mapOf(
            "userId" to DEFAULT_USER_ID,
            "fromDate" to from,
            "toDate" to to
        )

        return jdbc.queryForObject(sql, params) { rs, _ ->
            PeriodSummary(
                from = from,
                to = to,
                runCount = rs.getInt("run_count"),
                totalDistanceM = rs.getInt("total_distance_m"),
                totalMovingTimeS = rs.getInt("total_moving_time_s"),
                avgPaceSecPerKm = rs.getIntOrNull("avg_pace_sec_per_km"),
                avgHr = rs.getIntOrNull("avg_hr")
            )
        } ?: PeriodSummary(from, to, 0, 0, 0, null, null)
    }

    fun comparePeriods(aFrom: LocalDate, aTo: LocalDate, bFrom: LocalDate, bTo: LocalDate): PeriodComparison {
        val a = getPeriodSummary(aFrom, aTo)
        val b = getPeriodSummary(bFrom, bTo)

        fun deltaOrNull(aVal: Int?, bVal: Int?): Int? =
            if (aVal != null && bVal != null) aVal - bVal else null

        return PeriodComparison(
            a = a,
            b = b,
            deltaDistanceM = a.totalDistanceM - b.totalDistanceM,
            deltaAvgPaceSecPerKm = deltaOrNull(a.avgPaceSecPerKm, b.avgPaceSecPerKm),
            deltaAvgHr = deltaOrNull(a.avgHr, b.avgHr)
        )
    }




    fun getTrend(from: LocalDate, to: LocalDate, metric: TrendMetric): List<TrendPoint> {
        // 주 시작일(월요일 기준): stat_date - WEEKDAY(stat_date)
        val valueExpr = when (metric) {
            TrendMetric.TOTAL_DISTANCE_M -> "sum(total_distance_m)"
            TrendMetric.AVG_PACE_SEC_PER_KM -> """
            case
              when sum(total_moving_time_s) = 0 then null
              else floor(sum(coalesce(avg_pace_sec_per_km,0) * total_moving_time_s) / sum(total_moving_time_s))
            end
        """.trimIndent()
            TrendMetric.AVG_HR -> """
            case
              when sum(total_moving_time_s) = 0 then null
              else floor(sum(coalesce(avg_hr,0) * total_moving_time_s) / sum(total_moving_time_s))
            end
        """.trimIndent()
        }

        val sql = """
        select
          date_sub(stat_date, interval weekday(stat_date) day) as week_start,
          $valueExpr as value
        from daily_stats
        where user_id = :userId
          and stat_date >= :fromDate
          and stat_date <= :toDate
        group by week_start
        order by week_start asc
    """.trimIndent()

        val params = mapOf(
            "userId" to DEFAULT_USER_ID,
            "fromDate" to from,
            "toDate" to to
        )

        return jdbc.query(sql, params) { rs, _ ->
            TrendPoint(
                bucketStart = rs.getDate("week_start").toLocalDate(),
                value = rs.getIntOrNull("value")
            )
        }
    }

    fun getAnomalyRuns(from: LocalDate, to: LocalDate, type: AnomalyType, limit: Int = 20): List<AnomalyRun> {
        // Phase 1: 가장 “쓸모 있는” 이상치 하나만 먼저
        // HIGH_HR_LOW_PACE: 평균심박 높고, 페이스 느린 러닝(피로/컨디션 저하 후보)
        val (hrThreshold, paceThreshold) = when (type) {
            AnomalyType.HIGH_HR_LOW_PACE -> 160 to 360 // 160bpm 이상 + 6:00/km 이상(= 느림)
        }

        val sql = """
        select
          a.activity_id,
          a.start_time_utc,
          a.distance_m,
          a.avg_pace_sec_per_km,
          a.avg_hr
        from activities a
        where a.user_id = :userId
          and a.sport_type = 'RUN'
          and a.start_time_utc >= :fromDt
          and a.start_time_utc < :toDtExclusive
          and a.avg_hr is not null
          and a.avg_pace_sec_per_km is not null
          and a.avg_hr >= :hrTh
          and a.avg_pace_sec_per_km >= :paceTh
        order by a.start_time_utc desc
        limit :limit
    """.trimIndent()

        val params = mapOf(
            "userId" to DEFAULT_USER_ID,
            "fromDt" to from.atStartOfDay(),
            "toDtExclusive" to to.plusDays(1).atStartOfDay(),
            "hrTh" to hrThreshold,
            "paceTh" to paceThreshold,
            "limit" to limit
        )

        return jdbc.query(sql, params) { rs, _ ->
            val startTime = rs.getObject("start_time_utc", LocalDateTime::class.java)
            AnomalyRun(
                activityId = rs.getLong("activity_id"),
                date = startTime.toLocalDate(),
                distanceM = rs.getInt("distance_m"),
                avgPaceSecPerKm = rs.getIntOrNull("avg_pace_sec_per_km"),
                avgHr = rs.getIntOrNull("avg_hr"),
                reason = "avg_hr >= $hrThreshold AND avg_pace_sec_per_km >= $paceThreshold"
            )
        }
    }

    /**
     * Phase 1: 단일 사용자 가정(한수 님)
     * 나중에 userId를 Tool 파라미터로 받거나 Auth에서 주입
     */
    companion object {
        const val DEFAULT_USER_ID: Long = 1L
    }
}

private fun ResultSet.toActivitySummary(): ActivitySummary {
    val startTime = getObject("start_time_utc", LocalDateTime::class.java)
    return ActivitySummary(
        activityId = getLong("activity_id"),
        date = startTime.toLocalDate(),
        distanceM = getInt("distance_m"),
        movingTimeS = getInt("moving_time_s"),
        avgPaceSecPerKm = getIntOrNull("avg_pace_sec_per_km"),
        avgHr = getIntOrNull("avg_hr")
    )
}

private fun ResultSet.getIntOrNull(column: String): Int? {
    val v = getInt(column)
    return if (wasNull()) null else v
}