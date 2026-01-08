package io.hansu.pacer.util

import dev.langchain4j.agent.tool.Tool
import io.hansu.pacer.dto.ActivitySummary
import io.hansu.pacer.dto.AnomalyRun
import io.hansu.pacer.dto.AnomalyType
import io.hansu.pacer.dto.PeriodComparison
import io.hansu.pacer.dto.PeriodSummary
import io.hansu.pacer.dto.TrendMetric
import io.hansu.pacer.dto.TrendPoint
import io.hansu.pacer.service.RunningQueryService
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class RunningTools(
    private val runningQueryService: RunningQueryService
) {
    @Tool("기간 조건으로 러닝 활동 목록을 조회합니다.")
    fun listActivities(from: LocalDate, to: LocalDate, limit: Int = 30): List<ActivitySummary> =
        runningQueryService.listActivities(from, to, limit)

    @Tool("특정 기간의 러닝 요약(거리/시간/평균 페이스/평균 심박)을 조회합니다.")
    fun getPeriodSummary(from: LocalDate, to: LocalDate): PeriodSummary =
        runningQueryService.getPeriodSummary(from, to)

    @Tool("두 기간의 러닝 요약을 비교합니다.")
    fun comparePeriods(aFrom: LocalDate, aTo: LocalDate, bFrom: LocalDate, bTo: LocalDate): PeriodComparison =
        runningQueryService.comparePeriods(aFrom, aTo, bFrom, bTo)

    @Tool("기간의 주간 추세를 조회합니다. metric은 TOTAL_DISTANCE_M, AVG_PACE_SEC_PER_KM, AVG_HR 중 하나입니다.")
    fun getTrend(from: LocalDate, to: LocalDate, metric: TrendMetric): List<TrendPoint> =
        runningQueryService.getTrend(from, to, metric)

    @Tool("기간 내 이상치 러닝을 조회합니다. 현재 지원: HIGH_HR_LOW_PACE")
    fun getAnomalyRuns(from: LocalDate, to: LocalDate, anomalyType: AnomalyType, limit: Int = 20): List<AnomalyRun> =
        runningQueryService.getAnomalyRuns(from, to, anomalyType, limit)
}