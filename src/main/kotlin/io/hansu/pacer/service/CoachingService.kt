package io.hansu.pacer.service

import io.hansu.pacer.domain.goal.GoalStatus
import io.hansu.pacer.domain.goal.repository.RaceGoalRepository
import io.hansu.pacer.domain.training.repository.TrainingLoadRepository
import io.hansu.pacer.dto.*
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class CoachingService(
    private val raceGoalRepository: RaceGoalRepository,
    private val trainingLoadRepository: TrainingLoadRepository,
    private val vdotCalculator: VdotCalculator,
    private val runningQueryService: RunningQueryService
) {

    fun getCoachingAdvice(userId: Long, today: LocalDate = LocalDate.now()): CoachingResponse {
        val goal = raceGoalRepository.findFirstByUserIdAndStatusOrderByRaceDateAsc(userId, GoalStatus.ACTIVE)
        
        // 1. Calculate Current VDOT & Fitness
        // (For MVP, using recent 30 days best 5K or 10K equivalent logic is too complex without DB support.
        // Instead, using recent 4 weeks average pace as proxy if no TrainingLoad exists yet)
        
        // TODO: In future, use real VDOT from best efforts. Here we estimate from recent average pace.
        val recentSummary = runningQueryService.getPeriodSummary(userId, today.minusDays(28), today)
        val currentVdot = if (recentSummary.avgPaceSecPerKm != null && recentSummary.avgPaceSecPerKm > 0) {
            // Rough estimate: Assume avg pace is Easy pace (approx 75% intensity)
            // Reverse engineering: Threshold pace ~ AvgPace / 1.25
            val estimatedThresholdPace = recentSummary.avgPaceSecPerKm / 1.25
            // Convert to VDOT (This is a placeholder heuristic)
            // VDOT 50 -> Threshold 240s/km. VDOT = 50 * (240 / estThreshold)
            50.0 * (240.0 / estimatedThresholdPace)
        } else {
            30.0 // Default for beginners
        }

        // 2. Training Paces
        val paces = vdotCalculator.calculateTrainingPaces(currentVdot)
        val paceStrings = paces.mapValues { formatPace(it.value) }

        // 3. Fitness Status (Mock for now, will implement real TSB later)
        val fitnessStatus = FitnessStatus(
            ctl = 45.0,
            atl = 50.0,
            tsb = -5.0,
            status = "Productive"
        )

        // 4. Generate Plan
        val plan = if (goal != null) {
            generatePlanForGoal(goal.raceDate, today, goal.raceDistanceM, currentVdot)
        } else {
            generateMaintenancePlan(today, currentVdot)
        }

        val goalDto = goal?.let {
            RaceGoalDto(
                raceName = it.raceName,
                raceDate = it.raceDate,
                remainingWeeks = ChronoUnit.WEEKS.between(today, it.raceDate).toInt(),
                targetPace = formatPace(it.targetPaceSecPerKm)
            )
        }

        val advice = if (goal != null) {
            "목표인 ${goal.raceName}까지 ${goalDto?.remainingWeeks}주 남았습니다. 현재 컨디션은 생산적이며, 이번 주는 지구력 향상에 집중하세요."
        } else {
            "현재 설정된 레이스 목표가 없습니다. 꾸준한 이지런으로 기초 체력을 다지는 것을 추천합니다."
        }

        return CoachingResponse(
            raceGoal = goalDto,
            currentVdot = String.format("%.1f", currentVdot).toDouble(),
            trainingPaces = paceStrings,
            fitnessStatus = fitnessStatus,
            weeklyPlan = plan,
            recommendation = advice
        )
    }

    private fun generatePlanForGoal(raceDate: LocalDate, today: LocalDate, distanceM: Int, vdot: Double): List<DailyPlan> {
        val weeksUntilRace = ChronoUnit.WEEKS.between(today, raceDate).toInt()
        val plan = mutableListOf<DailyPlan>()
        
        // Simple logic: 4 week cycle
        // If close to race (<= 2 weeks): Taper
        // Else: Build
        
        for (i in 0..6) {
            val date = today.plusDays(i.toLong())
            val dayOfWeek = date.dayOfWeek.name
            
            val (type, desc, dist) = when (dayOfWeek) {
                "MONDAY" -> Triple("REST", "휴식 또는 가벼운 스트레칭", 0.0)
                "TUESDAY" -> Triple("EASY", "가벼운 조깅 (Easy Pace)", 5.0)
                "WEDNESDAY" -> Triple("INTERVAL", "400m 인터벌 5회", 7.0)
                "THURSDAY" -> Triple("EASY", "회복 러닝", 5.0)
                "FRIDAY" -> Triple("REST", "완전 휴식", 0.0)
                "SATURDAY" -> Triple("LONG_RUN", "LSD 장거리주", 10.0 + (weeksUntilRace % 3) * 2) // Progressive load
                "SUNDAY" -> Triple("RECOVERY", "가벼운 조깅", 4.0)
                else -> Triple("REST", "Rest", 0.0)
            }
            
            plan.add(DailyPlan(date, dayOfWeek, type, desc, dist))
        }
        return plan
    }

    private fun generateMaintenancePlan(today: LocalDate, vdot: Double): List<DailyPlan> {
        return (0..6).map { i ->
            val date = today.plusDays(i.toLong())
            DailyPlan(date, date.dayOfWeek.name, "EASY", "유지 관리 러닝", 5.0)
        }
    }

    private fun formatPace(secondsPerKm: Int): String {
        val min = secondsPerKm / 60
        val sec = secondsPerKm % 60
        return "%d:%02d/km".format(min, sec)
    }
}
