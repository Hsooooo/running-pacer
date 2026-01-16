package io.hansu.pacer.dto

import java.time.LocalDate

data class RunningAnalysisResult(
    val analysisType: String,
    val period: PeriodInfo,
    val metrics: MetricsSummary,
    val insights: List<Insight>,
    val recommendations: List<Recommendation>,
    val riskScore: Int?
)

data class PeriodInfo(
    val from: LocalDate,
    val to: LocalDate,
    val totalDays: Int
)

data class MetricsSummary(
    val totalRuns: Int,
    val totalDistanceKm: Double,
    val totalDurationMinutes: Int,
    val avgPaceMinPerKm: Double?,
    val avgHeartRate: Int?,
    val weeklyAvgDistanceKm: Double
)

data class Insight(
    val type: InsightType,
    val severity: Severity,
    val title: String,
    val description: String,
    val evidence: Map<String, Any>?
)

enum class InsightType {
    PROGRESS, REGRESSION, ANOMALY, CONSISTENCY, OVERTRAINING, UNDERTRAINING
}

enum class Severity {
    INFO, WARNING, CRITICAL
}

data class Recommendation(
    val priority: Int,
    val category: String,
    val action: String,
    val reason: String
)

data class TrendAnalysisResult(
    val metric: String,
    val period: PeriodInfo,
    val dataPoints: List<TrendDataPoint>,
    val trend: TrendDirection,
    val changePercent: Double?,
    val interpretation: String
)

data class TrendDataPoint(
    val date: LocalDate,
    val value: Double?,
    val label: String?
)

enum class TrendDirection {
    IMPROVING, DECLINING, STABLE, INSUFFICIENT_DATA
}

data class ComparisonResult(
    val periodA: PeriodSummaryWithLabel,
    val periodB: PeriodSummaryWithLabel,
    val deltas: Map<String, DeltaValue>,
    val winner: String?,
    val interpretation: String
)

data class PeriodSummaryWithLabel(
    val label: String,
    val summary: PeriodSummary
)

data class DeltaValue(
    val absolute: Double,
    val percent: Double?,
    val interpretation: String
)
