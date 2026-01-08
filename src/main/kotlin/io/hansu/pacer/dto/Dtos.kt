package io.hansu.pacer.dto

import java.time.LocalDate

data class ActivitySummary(
    val activityId: Long,
    val date: LocalDate,
    val distanceM: Int,
    val movingTimeS: Int,
    val avgPaceSecPerKm: Int?,
    val avgHr: Int?
)

data class PeriodSummary(
    val from: LocalDate,
    val to: LocalDate,
    val runCount: Int,
    val totalDistanceM: Int,
    val totalMovingTimeS: Int,
    val avgPaceSecPerKm: Int?,
    val avgHr: Int?
)

data class PeriodComparison(
    val a: PeriodSummary,
    val b: PeriodSummary,
    val deltaDistanceM: Int,
    val deltaAvgPaceSecPerKm: Int?,
    val deltaAvgHr: Int?
)

/** (선택) UI/로그에 쓰기 좋은 유틸 */
fun Int.toKmString(): String = String.format("%.2fkm", this / 1000.0)

data class TrendPoint(
    val bucketStart: LocalDate,
    val value: Int? // metric에 따라 의미가 달라짐(예: avg_pace_sec_per_km)
)

enum class TrendMetric {
    TOTAL_DISTANCE_M,
    AVG_PACE_SEC_PER_KM,
    AVG_HR
}

data class AnomalyRun(
    val activityId: Long,
    val date: LocalDate,
    val distanceM: Int,
    val avgPaceSecPerKm: Int?,
    val avgHr: Int?,
    val reason: String
)

enum class AnomalyType {
    HIGH_HR_LOW_PACE
}
