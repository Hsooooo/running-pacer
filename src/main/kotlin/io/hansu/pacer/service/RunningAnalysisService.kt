package io.hansu.pacer.service

import io.hansu.pacer.dto.*
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class RunningAnalysisService(
    private val queryService: RunningQueryService
) {
    fun analyzePerformance(
        userId: Long,
        from: LocalDate,
        to: LocalDate,
        includeRecommendations: Boolean
    ): RunningAnalysisResult {
        val summary = queryService.getPeriodSummary(userId, from, to)
        val anomalies = queryService.getAnomalyRuns(userId, from, to, AnomalyType.HIGH_HR_LOW_PACE, 10)
        val totalDays = ChronoUnit.DAYS.between(from, to).toInt() + 1
        val weeks = totalDays / 7.0

        val metrics = MetricsSummary(
            totalRuns = summary.runCount,
            totalDistanceKm = summary.totalDistanceM / 1000.0,
            totalDurationMinutes = summary.totalMovingTimeS / 60,
            avgPaceMinPerKm = summary.avgPaceSecPerKm?.let { it / 60.0 },
            avgHeartRate = summary.avgHr,
            weeklyAvgDistanceKm = if (weeks > 0) summary.totalDistanceM / 1000.0 / weeks else 0.0
        )

        val insights = mutableListOf<Insight>()

        if (metrics.weeklyAvgDistanceKm < 15) {
            insights.add(
                Insight(
                    type = InsightType.UNDERTRAINING,
                    severity = Severity.INFO,
                    title = "주간 러닝 볼륨이 낮습니다",
                    description = "주당 평균 ${String.format("%.1f", metrics.weeklyAvgDistanceKm)}km로, 체력 유지를 위한 권장량(20-30km)에 미달합니다.",
                    evidence = mapOf("weeklyAvgKm" to metrics.weeklyAvgDistanceKm)
                )
            )
        }

        if (anomalies.isNotEmpty()) {
            insights.add(
                Insight(
                    type = InsightType.ANOMALY,
                    severity = if (anomalies.size >= 3) Severity.WARNING else Severity.INFO,
                    title = "피로 징후가 감지된 러닝 ${anomalies.size}건",
                    description = "높은 심박수와 느린 페이스가 동시에 나타난 러닝이 있습니다. 컨디션 관리가 필요할 수 있습니다.",
                    evidence = mapOf("count" to anomalies.size, "dates" to anomalies.map { it.date.toString() })
                )
            )
        }

        val riskScore = calculateRiskScore(metrics, anomalies.size)

        val recommendations = if (includeRecommendations) {
            generateRecommendations(metrics, riskScore)
        } else emptyList()

        return RunningAnalysisResult(
            analysisType = "PERFORMANCE_ANALYSIS",
            period = PeriodInfo(from, to, totalDays),
            metrics = metrics,
            insights = insights,
            recommendations = recommendations,
            riskScore = riskScore
        )
    }

    fun comparePeriods(userId: Long, aFrom: LocalDate, aTo: LocalDate, bFrom: LocalDate, bTo: LocalDate): ComparisonResult {
        val summaryA = queryService.getPeriodSummary(userId, aFrom, aTo)
        val summaryB = queryService.getPeriodSummary(userId, bFrom, bTo)

        val distanceDelta = summaryA.totalDistanceM - summaryB.totalDistanceM
        val paceDelta = if (summaryA.avgPaceSecPerKm != null && summaryB.avgPaceSecPerKm != null) {
            summaryB.avgPaceSecPerKm - summaryA.avgPaceSecPerKm
        } else null

        val deltas = mutableMapOf<String, DeltaValue>()

        deltas["distance"] = DeltaValue(
            absolute = distanceDelta / 1000.0,
            percent = if (summaryB.totalDistanceM > 0) distanceDelta * 100.0 / summaryB.totalDistanceM else null,
            interpretation = when {
                distanceDelta > 0 -> "better"
                distanceDelta < 0 -> "worse"
                else -> "same"
            }
        )

        if (paceDelta != null) {
            deltas["pace"] = DeltaValue(
                absolute = paceDelta / 60.0,
                percent = summaryB.avgPaceSecPerKm?.let { paceDelta * 100.0 / it },
                interpretation = when {
                    paceDelta > 0 -> "better"
                    paceDelta < 0 -> "worse"
                    else -> "same"
                }
            )
        }

        val winner = when {
            distanceDelta > 0 && (paceDelta == null || paceDelta >= 0) -> "A"
            distanceDelta < 0 && (paceDelta == null || paceDelta <= 0) -> "B"
            else -> null
        }

        return ComparisonResult(
            periodA = PeriodSummaryWithLabel("기간 A", summaryA),
            periodB = PeriodSummaryWithLabel("기간 B", summaryB),
            deltas = deltas,
            winner = winner,
            interpretation = generateComparisonInterpretation(deltas, winner)
        )
    }

    fun analyzeTrend(userId: Long, from: LocalDate, to: LocalDate, metric: TrendMetric): TrendAnalysisResult {
        val trendPoints = queryService.getTrend(userId, from, to, metric)

        val values = trendPoints.mapNotNull { it.value?.toDouble() }
        val trend = if (values.size < 2) {
            TrendDirection.INSUFFICIENT_DATA
        } else {
            val firstHalf = values.take(values.size / 2).average()
            val secondHalf = values.takeLast(values.size / 2).average()
            val change = secondHalf - firstHalf

            when {
                metric == TrendMetric.AVG_PACE_SEC_PER_KM -> {
                    when {
                        change < -10 -> TrendDirection.IMPROVING
                        change > 10 -> TrendDirection.DECLINING
                        else -> TrendDirection.STABLE
                    }
                }
                else -> {
                    when {
                        change > values.average() * 0.05 -> TrendDirection.IMPROVING
                        change < -values.average() * 0.05 -> TrendDirection.DECLINING
                        else -> TrendDirection.STABLE
                    }
                }
            }
        }

        val changePercent = if (values.size >= 2) {
            val first = values.first()
            val last = values.last()
            if (first > 0) ((last - first) / first) * 100 else null
        } else null

        return TrendAnalysisResult(
            metric = metric.name,
            period = PeriodInfo(from, to, ChronoUnit.DAYS.between(from, to).toInt() + 1),
            dataPoints = trendPoints.map { TrendDataPoint(it.bucketStart, it.value?.toDouble(), null) },
            trend = trend,
            changePercent = changePercent,
            interpretation = generateTrendInterpretation(metric, trend, changePercent)
        )
    }

    private fun calculateRiskScore(metrics: MetricsSummary, anomalyCount: Int): Int {
        var score = 0

        if (metrics.weeklyAvgDistanceKm > 80) score += 30
        else if (metrics.weeklyAvgDistanceKm > 60) score += 15

        score += anomalyCount * 10

        metrics.avgHeartRate?.let {
            if (it > 170) score += 20
            else if (it > 160) score += 10
        }

        return minOf(score, 100)
    }

    private fun generateRecommendations(metrics: MetricsSummary, riskScore: Int): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()

        if (riskScore >= 50) {
            recommendations.add(
                Recommendation(
                    priority = 1,
                    category = "RECOVERY",
                    action = "1-2일 휴식 또는 저강도 러닝을 권장합니다",
                    reason = "피로 위험 점수가 높습니다 ($riskScore/100)"
                )
            )
        }

        if (metrics.weeklyAvgDistanceKm < 15) {
            recommendations.add(
                Recommendation(
                    priority = 2,
                    category = "VOLUME",
                    action = "주간 러닝 횟수를 1-2회 늘려보세요",
                    reason = "현재 주당 평균 ${String.format("%.1f", metrics.weeklyAvgDistanceKm)}km로 권장량에 미달합니다"
                )
            )
        }

        return recommendations.sortedBy { it.priority }
    }

    private fun generateComparisonInterpretation(deltas: Map<String, DeltaValue>, winner: String?): String {
        val distanceDelta = deltas["distance"]
        return when (winner) {
            "A" -> "기간 A가 더 좋은 성과를 보였습니다. 거리가 ${String.format("%.1f", distanceDelta?.absolute)}km 더 많습니다."
            "B" -> "기간 B가 더 좋은 성과를 보였습니다."
            else -> "두 기간의 성과가 비슷합니다."
        }
    }

    private fun generateTrendInterpretation(metric: TrendMetric, trend: TrendDirection, changePercent: Double?): String {
        val metricName = when (metric) {
            TrendMetric.TOTAL_DISTANCE_M -> "러닝 거리"
            TrendMetric.AVG_PACE_SEC_PER_KM -> "평균 페이스"
            TrendMetric.AVG_HR -> "평균 심박수"
        }

        return when (trend) {
            TrendDirection.IMPROVING -> "$metricName 이 개선되고 있습니다. ${changePercent?.let { "(${String.format("%.1f", it)}% 변화)" } ?: ""}"
            TrendDirection.DECLINING -> "$metricName 이 하락하고 있습니다. 주의가 필요합니다."
            TrendDirection.STABLE -> "$metricName 이 안정적으로 유지되고 있습니다."
            TrendDirection.INSUFFICIENT_DATA -> "추세 분석을 위한 데이터가 부족합니다."
        }
    }
}
