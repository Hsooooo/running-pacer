package io.hansu.pacer.service

import io.hansu.pacer.domain.goal.GoalStatus
import io.hansu.pacer.domain.goal.repository.RaceGoalRepository
import io.hansu.pacer.dto.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import org.springframework.stereotype.Service

@Service
class CoachingService(
        private val raceGoalRepository: RaceGoalRepository,
        private val vdotCalculator: VdotCalculator,
        private val trainingBalanceService: TrainingBalanceService
) {

    fun getCoachingAdvice(userId: Long, today: LocalDate = LocalDate.now()): CoachingResponse {
        val goal =
                raceGoalRepository.findFirstByUserIdAndStatusOrderByRaceDateAsc(
                        userId,
                        GoalStatus.ACTIVE
                )

        // 1. Best Effort 기반 정확한 VDOT 계산
        val currentVdot = vdotCalculator.getCurrentVdot(userId)
        val bestEfforts = vdotCalculator.getBestEfforts(userId)

        // 2. Training Paces (VDOT 기반)
        val paces = vdotCalculator.calculateTrainingPaces(currentVdot)
        val paceStrings = paces.mapValues { formatPace(it.value) }

        // 3. 실제 피트니스 상태 계산 (CTL/ATL/TSB)
        val fitnessStatus = trainingBalanceService.getFitnessStatus(userId, today)

        // 4. Generate Plan
        val plan =
                if (goal != null) {
                    generatePlanForGoal(
                            goal.raceDate,
                            today,
                            goal.raceDistanceM,
                            currentVdot,
                            fitnessStatus
                    )
                } else {
                    generateMaintenancePlan(today, currentVdot, fitnessStatus)
                }

        val goalDto =
                goal?.let {
                    RaceGoalDto(
                            raceName = it.raceName,
                            raceDate = it.raceDate,
                            remainingWeeks = ChronoUnit.WEEKS.between(today, it.raceDate).toInt(),
                            targetPace = formatPace(it.targetPaceSecPerKm)
                    )
                }

        // 5. 컨디션 기반 맞춤 조언 생성
        val advice = generateAdvice(goal, goalDto, fitnessStatus, bestEfforts)

        return CoachingResponse(
                raceGoal = goalDto,
                currentVdot = String.format("%.1f", currentVdot).toDouble(),
                trainingPaces = paceStrings,
                fitnessStatus = fitnessStatus,
                weeklyPlan = plan,
                recommendation = advice
        )
    }

    /** 피트니스 상태와 목표에 따른 맞춤 조언 생성 */
    private fun generateAdvice(
            goal: io.hansu.pacer.domain.goal.RaceGoalEntity?,
            goalDto: RaceGoalDto?,
            fitness: FitnessStatus,
            bestEfforts: List<BestEffort>
    ): String {
        val fitnessAdvice =
                when (fitness.status) {
                    "Rested" -> "충분히 회복된 상태입니다. 고강도 훈련을 시도해볼 좋은 시점입니다."
                    "Fresh" -> "컨디션이 좋습니다. 계획된 훈련을 진행하세요."
                    "Productive" -> "생산적인 훈련 상태입니다. 현재 패턴을 유지하세요."
                    "Tired" -> "피로가 누적되고 있습니다. 이번 주는 Easy 런 위주로 회복하세요."
                    "Overreached" -> "⚠️ 과훈련 위험! 2-3일 완전 휴식을 강력히 권장합니다."
                    else -> ""
                }

        return if (goal != null) {
            val weekInfo = "목표인 ${goal.raceName}까지 ${goalDto?.remainingWeeks}주 남았습니다."
            val vdotInfo =
                    if (bestEfforts.isNotEmpty()) {
                        val best = bestEfforts.maxByOrNull { it.vdot }
                        " 최근 ${best?.distanceType?.displayName} 기록 기준 VDOT ${String.format("%.1f", best?.vdot)}입니다."
                    } else ""
            "$weekInfo$vdotInfo $fitnessAdvice"
        } else {
            "현재 설정된 레이스 목표가 없습니다. $fitnessAdvice"
        }
    }

    private fun generatePlanForGoal(
            raceDate: LocalDate,
            today: LocalDate,
            distanceM: Int,
            vdot: Double,
            fitness: FitnessStatus
    ): List<DailyPlan> {
        val weeksUntilRace = ChronoUnit.WEEKS.between(today, raceDate).toInt()
        val plan = mutableListOf<DailyPlan>()

        // 피로 상태에 따른 강도 조절
        val needsRecovery = fitness.tsb < -15 || fitness.status in listOf("Tired", "Overreached")

        for (i in 0..6) {
            val date = today.plusDays(i.toLong())
            val dayOfWeek = date.dayOfWeek.name

            val (type, desc, dist) =
                    if (needsRecovery) {
                        // 피로 상태: 회복 위주 계획
                        when (dayOfWeek) {
                            "MONDAY" -> Triple("REST", "완전 휴식", 0.0)
                            "TUESDAY" -> Triple("RECOVERY", "가벼운 조깅 (회복 우선)", 4.0)
                            "WEDNESDAY" -> Triple("EASY", "이지 페이스 러닝", 5.0)
                            "THURSDAY" -> Triple("REST", "휴식", 0.0)
                            "FRIDAY" -> Triple("RECOVERY", "가벼운 조깅", 4.0)
                            "SATURDAY" -> Triple("EASY", "이지 런 (Long Run 대체)", 8.0)
                            "SUNDAY" -> Triple("REST", "완전 휴식", 0.0)
                            else -> Triple("REST", "Rest", 0.0)
                        }
                    } else {
                        // 정상 상태: 기존 계획
                        when (dayOfWeek) {
                            "MONDAY" -> Triple("REST", "휴식 또는 가벼운 스트레칭", 0.0)
                            "TUESDAY" -> Triple("EASY", "가벼운 조깅 (Easy Pace)", 5.0)
                            "WEDNESDAY" -> Triple("INTERVAL", "400m 인터벌 5회", 7.0)
                            "THURSDAY" -> Triple("EASY", "회복 러닝", 5.0)
                            "FRIDAY" -> Triple("REST", "완전 휴식", 0.0)
                            "SATURDAY" ->
                                    Triple("LONG_RUN", "LSD 장거리주", 10.0 + (weeksUntilRace % 3) * 2)
                            "SUNDAY" -> Triple("RECOVERY", "가벼운 조깅", 4.0)
                            else -> Triple("REST", "Rest", 0.0)
                        }
                    }

            plan.add(DailyPlan(date, dayOfWeek, type, desc, dist))
        }
        return plan
    }

    private fun generateMaintenancePlan(
            today: LocalDate,
            vdot: Double,
            fitness: FitnessStatus
    ): List<DailyPlan> {
        val needsRecovery = fitness.tsb < -15 || fitness.status in listOf("Tired", "Overreached")

        return (0..6).map { i ->
            val date = today.plusDays(i.toLong())
            val dayOfWeek = date.dayOfWeek.name

            if (needsRecovery) {
                // 피로 상태: 격일 러닝
                if (i % 2 == 0) {
                    DailyPlan(date, dayOfWeek, "RECOVERY", "가벼운 회복 러닝", 4.0)
                } else {
                    DailyPlan(date, dayOfWeek, "REST", "휴식", 0.0)
                }
            } else {
                DailyPlan(date, dayOfWeek, "EASY", "유지 관리 러닝", 5.0)
            }
        }
    }

    private fun formatPace(secondsPerKm: Int): String {
        val min = secondsPerKm / 60
        val sec = secondsPerKm % 60
        return "%d:%02d/km".format(min, sec)
    }
}
