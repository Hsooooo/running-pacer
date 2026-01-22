package io.hansu.pacer.controller

import io.hansu.pacer.dto.DashboardInsights
import io.hansu.pacer.dto.WorkoutDistributionDto
import io.hansu.pacer.service.RunningToolService
import io.hansu.pacer.service.TrainingBalanceService
import io.hansu.pacer.service.VdotCalculator
import io.hansu.pacer.service.RunningIndexService
import java.time.DayOfWeek
import java.time.LocalDate
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/dashboard")
class DashboardController(
        private val toolService: RunningToolService,
        private val vdotCalculator: VdotCalculator,
        private val trainingBalanceService: TrainingBalanceService,
        private val runningIndexService: RunningIndexService
) {

    @GetMapping("/insights")
    fun getInsights(
            @AuthenticationPrincipal principal: OAuth2User?
    ): ResponseEntity<DashboardInsights> {
        val userId = extractUserId(principal) ?: return ResponseEntity.status(401).build()

        val today = LocalDate.now()
        val weekStart = today.with(DayOfWeek.MONDAY)

        val weeklySummary = toolService.getPeriodSummary(userId, weekStart, today)

        val trend =
                toolService.analyzeTrend(
                        userId,
                        mapOf(
                                "from" to today.minusWeeks(4).toString(),
                                "to" to today.toString(),
                                "metric" to "TOTAL_DISTANCE_M"
                        )
                )

        val anomalies = toolService.getAnomalyRuns(userId, today.minusDays(30), today)

        val performance =
                toolService.analyzePerformance(
                        userId,
                        mapOf("from" to today.minusDays(30).toString(), "to" to today.toString())
                )

        // New training metrics
        val currentVdot =
                try {
                    vdotCalculator.getCurrentVdot(userId)
                } catch (e: Exception) {
                    null
                }
        val bestEfforts =
                try {
                    vdotCalculator.getBestEfforts(userId)
                } catch (e: Exception) {
                    emptyList()
                }
        val trainingPaces =
                currentVdot?.let {
                    vdotCalculator.calculateTrainingPaces(it).mapValues { (_, secs) ->
                        formatPace(secs)
                    }
                }
        val fitnessStatus =
                try {
                    trainingBalanceService.getFitnessStatus(userId, today)
                } catch (e: Exception) {
                    null
                }
        val distribution =
                try {
                    val d =
                            trainingBalanceService.getWorkoutDistribution(
                                    userId,
                                    today.minusWeeks(4),
                                    today
                            )
                    WorkoutDistributionDto(
                            totalRuns = d.totalRuns,
                            counts = d.counts,
                            percentages = d.percentages,
                            balanceScore = d.balance.score,
                            balanceAssessment = d.balance.assessment,
                            easyHardRatio = d.balance.easyHardRatio,
                            recommendations = d.balance.recommendations
                    )
                } catch (e: Exception) {
                    null
                }

        return ResponseEntity.ok(
                DashboardInsights(
                        weeklySummary = weeklySummary,
                        trend = trend,
                        anomaliesCount = anomalies.size,
                        riskScore = performance.riskScore ?: 0,
                        riskLevel =
                                when {
                                    (performance.riskScore ?: 0) >= 60 -> "HIGH"
                                    (performance.riskScore ?: 0) >= 30 -> "MEDIUM"
                                    else -> "LOW"
                                },
                        currentVdot = currentVdot,
                        bestEfforts = bestEfforts.ifEmpty { null },
                        fitnessStatus = fitnessStatus,
                        workoutDistribution = distribution,
                        trainingPaces = trainingPaces
                )
        )
    }

    private fun formatPace(secondsPerKm: Int): String {
        val min = secondsPerKm / 60
        val sec = secondsPerKm % 60
        return "%d:%02d/km".format(min, sec)
    }

    @GetMapping("/weather")
    fun getWeather(): ResponseEntity<Map<String, Any?>> {
        val currentIndex = runningIndexService.getCurrentRunningIndex()
        val hourlyIndex = runningIndexService.getHourlyRunningIndex()

        return ResponseEntity.ok(mapOf(
            "current" to currentIndex,
            "hourly" to hourlyIndex
        ))
    }

    private fun extractUserId(principal: OAuth2User?): Long? {
        val raw = principal?.attributes?.get("userId") ?: return null
        return when (raw) {
            is Number -> raw.toLong()
            is String -> raw.toLongOrNull()
            else -> null
        }
    }
}
