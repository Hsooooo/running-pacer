package io.hansu.pacer.util

import io.hansu.pacer.dto.ActivitySummary
import io.hansu.pacer.dto.AnomalyRun
import io.hansu.pacer.dto.AnomalyType
import io.hansu.pacer.dto.PeriodComparison
import io.hansu.pacer.dto.PeriodSummary
import io.hansu.pacer.dto.TrendMetric
import io.hansu.pacer.dto.TrendPoint
import io.hansu.pacer.mcp.McpUserContext
import io.hansu.pacer.service.RunningQueryService
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class RunningTools(
    private val runningQueryService: RunningQueryService,
    private val userContext: McpUserContext
) {
    fun listActivities(from: LocalDate, to: LocalDate, limit: Int = 30): List<ActivitySummary> =
        runningQueryService.listActivities(userContext.getCurrentUserId(), from, to, limit)

    fun getPeriodSummary(from: LocalDate, to: LocalDate): PeriodSummary =
        runningQueryService.getPeriodSummary(userContext.getCurrentUserId(), from, to)

    fun comparePeriods(aFrom: LocalDate, aTo: LocalDate, bFrom: LocalDate, bTo: LocalDate): PeriodComparison =
        runningQueryService.comparePeriods(userContext.getCurrentUserId(), aFrom, aTo, bFrom, bTo)

    fun getTrend(from: LocalDate, to: LocalDate, metric: TrendMetric): List<TrendPoint> =
        runningQueryService.getTrend(userContext.getCurrentUserId(), from, to, metric)

    fun getAnomalyRuns(from: LocalDate, to: LocalDate, anomalyType: AnomalyType, limit: Int = 20): List<AnomalyRun> =
        runningQueryService.getAnomalyRuns(userContext.getCurrentUserId(), from, to, anomalyType, limit)
}