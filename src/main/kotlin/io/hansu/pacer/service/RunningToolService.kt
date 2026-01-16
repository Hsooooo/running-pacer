package io.hansu.pacer.service

import io.hansu.pacer.dto.*
import io.hansu.pacer.domain.goal.GoalStatus
import io.hansu.pacer.domain.goal.RaceGoalEntity
import io.hansu.pacer.domain.goal.repository.RaceGoalRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class RunningToolService(
    private val queryService: RunningQueryService,
    private val analysisService: RunningAnalysisService,
    private val coachingService: CoachingService,
    private val raceGoalRepository: RaceGoalRepository
) {

    // ==========================================
    // Query Tools (Data Access)
    // ==========================================

    fun listActivities(userId: Long, from: LocalDate, to: LocalDate, limit: Int = 30): List<ActivitySummary> {
        return queryService.listActivities(userId, from, to, limit)
    }

    fun getPeriodSummary(userId: Long, from: LocalDate, to: LocalDate): PeriodSummary {
        return queryService.getPeriodSummary(userId, from, to)
    }

    fun getAnomalyRuns(userId: Long, from: LocalDate, to: LocalDate): List<AnomalyRun> {
        // Default to HIGH_HR_LOW_PACE
        return queryService.getAnomalyRuns(userId, from, to, AnomalyType.HIGH_HR_LOW_PACE)
    }

    // ==========================================
    // Insight Tools (Analysis & Coaching)
    // ==========================================

    fun comparePeriods(userId: Long, params: Map<String, Any>): ComparisonResult {
        // Default: Compare last week (A) vs previous week (B)
        val today = LocalDate.now()
        val defaultAFrom = today.minusDays(6)
        val defaultATo = today
        val defaultBFrom = today.minusDays(13)
        val defaultBTo = today.minusDays(7)

        val aFrom = params["period_a_from"]?.let { LocalDate.parse(it as String) } ?: defaultAFrom
        val aTo = params["period_a_to"]?.let { LocalDate.parse(it as String) } ?: defaultATo
        val bFrom = params["period_b_from"]?.let { LocalDate.parse(it as String) } ?: defaultBFrom
        val bTo = params["period_b_to"]?.let { LocalDate.parse(it as String) } ?: defaultBTo

        return analysisService.comparePeriods(userId, aFrom, aTo, bFrom, bTo)
    }

    fun analyzeTrend(userId: Long, params: Map<String, Any>): TrendAnalysisResult {
        // Default: Last 4 weeks, Distance metric
        val today = LocalDate.now()
        val defaultFrom = today.minusWeeks(4)
        val defaultTo = today

        val from = params["from"]?.let { LocalDate.parse(it as String) } ?: defaultFrom
        val to = params["to"]?.let { LocalDate.parse(it as String) } ?: defaultTo
        val metricStr = params["metric"] as? String ?: "TOTAL_DISTANCE_M"
        
        // Safety for invalid metric enum
        val metric = try {
            TrendMetric.valueOf(metricStr)
        } catch (e: Exception) {
            TrendMetric.TOTAL_DISTANCE_M
        }

        return analysisService.analyzeTrend(userId, from, to, metric)
    }

    fun analyzePerformance(userId: Long, params: Map<String, Any>): RunningAnalysisResult {
        // Default: Last 30 days
        val today = LocalDate.now()
        val defaultFrom = today.minusDays(30)
        val defaultTo = today

        val from = params["from"]?.let { LocalDate.parse(it as String) } ?: defaultFrom
        val to = params["to"]?.let { LocalDate.parse(it as String) } ?: defaultTo
        val includeRec = params["include_recommendations"] as? Boolean ?: true

        return analysisService.analyzePerformance(userId, from, to, includeRec)
    }

    fun getCoachingAdvice(userId: Long): CoachingResponse {
        return coachingService.getCoachingAdvice(userId)
    }

    fun setRaceGoal(userId: Long, params: Map<String, Any>): String {
        val raceName = params["race_name"] as? String ?: throw IllegalArgumentException("race_name is required")
        val raceDate = params["race_date"]?.let { LocalDate.parse(it as String) } 
            ?: throw IllegalArgumentException("race_date is required (YYYY-MM-DD)")
        
        val distanceM = (params["race_distance_m"] as? Number)?.toInt() 
            ?: throw IllegalArgumentException("race_distance_m is required")
            
        val targetPace = (params["target_pace_sec_per_km"] as? Number)?.toInt() 
            ?: throw IllegalArgumentException("target_pace_sec_per_km is required")

        // Deactivate old goals
        val oldGoals = raceGoalRepository.findByUserIdAndStatus(userId, GoalStatus.ACTIVE)
        oldGoals.forEach {
            it.status = GoalStatus.COMPLETED // or CANCELLED
            raceGoalRepository.save(it)
        }

        val newGoal = RaceGoalEntity(
            userId = userId,
            raceName = raceName,
            raceDate = raceDate,
            raceDistanceM = distanceM,
            targetPaceSecPerKm = targetPace,
            status = GoalStatus.ACTIVE
        )
        raceGoalRepository.save(newGoal)

        return "새로운 목표가 설정되었습니다: $raceName ($raceDate) - 목표 페이스 ${targetPace}초/km"
    }
}
