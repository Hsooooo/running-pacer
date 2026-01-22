package io.hansu.pacer.service

import io.hansu.pacer.domain.training.WorkoutType
import io.hansu.pacer.domain.training.repository.TrainingLoadRepository
import io.hansu.pacer.dto.FitnessStatus
import java.math.BigDecimal
import java.time.LocalDate
import org.springframework.stereotype.Service

/** 훈련 밸런스 및 피트니스 상태 분석 서비스 */
@Service
class TrainingBalanceService(
        private val trainingLoadRepo: TrainingLoadRepository,
        private val vdotCalculator: VdotCalculator
) {

    /**
     * 워크아웃 타입별 분포 분석
     * @return 각 타입별 런 횟수와 비율
     */
    fun getWorkoutDistribution(
            userId: Long,
            fromDate: LocalDate,
            toDate: LocalDate
    ): WorkoutDistribution {
        val rawData = trainingLoadRepo.getWorkoutTypeDistribution(userId, fromDate, toDate)

        val distribution = mutableMapOf<WorkoutType, Int>()
        var total = 0

        for (row in rawData) {
            val type = row[0] as WorkoutType
            val count = (row[1] as Long).toInt()
            distribution[type] = count
            total += count
        }

        // 비율 계산
        val percentages =
                distribution.mapValues { (_, count) ->
                    if (total > 0) (count.toDouble() / total * 100) else 0.0
                }

        // 훈련 밸런스 평가
        val balance = evaluateTrainingBalance(percentages)

        return WorkoutDistribution(
                period = PeriodRange(fromDate, toDate),
                totalRuns = total,
                counts = distribution,
                percentages = percentages,
                balance = balance
        )
    }

    /**
     * 피트니스 상태 계산 (CTL, ATL, TSB)
     * - CTL (Chronic Training Load): 42일 지수이동평균
     * - ATL (Acute Training Load): 7일 지수이동평균
     * - TSB (Training Stress Balance): CTL - ATL
     */
    fun getFitnessStatus(userId: Long, today: LocalDate = LocalDate.now()): FitnessStatus {
        val dailyTssData = trainingLoadRepo.getDailyTssSince(userId, today.minusDays(60))

        // 일별 TSS를 Map으로 변환 (없는 날은 0)
        val tssMap = mutableMapOf<LocalDate, Double>()
        for (row in dailyTssData) {
            val date = row[0] as LocalDate
            val tss = (row[1] as BigDecimal).toDouble()
            tssMap[date] = tss
        }

        // 지수이동평균 계산
        val ctl = calculateEMA(tssMap, today, 42)
        val atl = calculateEMA(tssMap, today, 7)
        val tsb = ctl - atl

        // 상태 판정
        val status =
                when {
                    tsb > 15 -> "Rested" // 충분히 회복된 상태
                    tsb > 0 -> "Fresh" // 가벼운 피로
                    tsb > -15 -> "Productive" // 생산적인 훈련 상태
                    tsb > -30 -> "Tired" // 피로 누적
                    else -> "Overreached" // 과훈련 위험
                }

        return FitnessStatus(
                ctl = String.format("%.1f", ctl).toDouble(),
                atl = String.format("%.1f", atl).toDouble(),
                tsb = String.format("%.1f", tsb).toDouble(),
                status = status
        )
    }

    /** 지수이동평균(EMA) 계산 EMA(today) = TSS(today) * k + EMA(yesterday) * (1-k) k = 2 / (n + 1) */
    private fun calculateEMA(
            tssMap: Map<LocalDate, Double>,
            endDate: LocalDate,
            days: Int
    ): Double {
        val k = 2.0 / (days + 1)
        var ema = 0.0

        // 과거부터 현재까지 순차적으로 계산
        for (i in days downTo 0) {
            val date = endDate.minusDays(i.toLong())
            val todayTss = tssMap[date] ?: 0.0
            ema = todayTss * k + ema * (1 - k)
        }

        return ema
    }

    /** 훈련 밸런스 평가 */
    private fun evaluateTrainingBalance(percentages: Map<WorkoutType, Double>): TrainingBalance {
        val easyPercent =
                (percentages[WorkoutType.EASY] ?: 0.0) + (percentages[WorkoutType.RECOVERY] ?: 0.0)
        val hardPercent =
                (percentages[WorkoutType.TEMPO] ?: 0.0) + (percentages[WorkoutType.INTERVAL] ?: 0.0)
        val longRunPercent = percentages[WorkoutType.LONG_RUN] ?: 0.0

        val recommendations = mutableListOf<String>()

        // 80/20 법칙 체크 (Easy 80%, Hard 20%)
        val score: Int
        val assessment: String

        when {
            easyPercent >= 75 && easyPercent <= 85 && hardPercent >= 15 && hardPercent <= 25 -> {
                score = 90
                assessment = "훌륭함"
            }
            easyPercent >= 70 && hardPercent <= 30 -> {
                score = 75
                assessment = "양호"
            }
            easyPercent < 60 -> {
                score = 50
                assessment = "개선 필요"
                recommendations.add(
                        "Easy 런 비중을 늘려주세요. 현재 ${String.format("%.0f", easyPercent)}%로 권장 비율(80%)에 미달합니다."
                )
            }
            hardPercent > 30 -> {
                score = 55
                assessment = "주의"
                recommendations.add(
                        "고강도 훈련 비중(${String.format("%.0f", hardPercent)}%)이 높습니다. 부상 위험이 있으니 Easy 런을 추가하세요."
                )
            }
            else -> {
                score = 65
                assessment = "보통"
            }
        }

        if (longRunPercent < 10) {
            recommendations.add("주간 Long Run이 부족합니다. 지구력 향상을 위해 주 1회 장거리 런을 권장합니다.")
        }

        return TrainingBalance(
                score = score,
                assessment = assessment,
                easyHardRatio =
                        "${String.format("%.0f", easyPercent)}/${String.format("%.0f", hardPercent)}",
                recommendations = recommendations
        )
    }

    /** 종합 훈련 분석 리포트 */
    fun getTrainingReport(userId: Long, weeks: Int = 4): TrainingReport {
        val today = LocalDate.now()
        val fromDate = today.minusWeeks(weeks.toLong())

        val distribution = getWorkoutDistribution(userId, fromDate, today)
        val fitness = getFitnessStatus(userId, today)
        val currentVdot = vdotCalculator.getCurrentVdot(userId)
        val bestEfforts = vdotCalculator.getBestEfforts(userId)
        val weeklyTss = trainingLoadRepo.getTotalTss(userId, fromDate, today) / weeks

        return TrainingReport(
                period = PeriodRange(fromDate, today),
                currentVdot = String.format("%.1f", currentVdot).toDouble(),
                bestEfforts = bestEfforts,
                workoutDistribution = distribution,
                fitnessStatus = fitness,
                weeklyAvgTss = String.format("%.1f", weeklyTss).toDouble()
        )
    }
}

// DTO들
data class PeriodRange(val from: LocalDate, val to: LocalDate)

data class WorkoutDistribution(
        val period: PeriodRange,
        val totalRuns: Int,
        val counts: Map<WorkoutType, Int>,
        val percentages: Map<WorkoutType, Double>,
        val balance: TrainingBalance
)

data class TrainingBalance(
        val score: Int,
        val assessment: String,
        val easyHardRatio: String,
        val recommendations: List<String>
)

data class TrainingReport(
        val period: PeriodRange,
        val currentVdot: Double,
        val bestEfforts: List<BestEffort>,
        val workoutDistribution: WorkoutDistribution,
        val fitnessStatus: FitnessStatus,
        val weeklyAvgTss: Double
)
