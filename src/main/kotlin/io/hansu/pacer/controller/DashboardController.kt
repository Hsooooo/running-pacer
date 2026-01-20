package io.hansu.pacer.controller

import io.hansu.pacer.dto.DashboardInsights
import io.hansu.pacer.service.RunningToolService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.DayOfWeek
import java.time.LocalDate

@RestController
@RequestMapping("/api/dashboard")
class DashboardController(
    private val toolService: RunningToolService
) {

    @GetMapping("/insights")
    fun getInsights(@AuthenticationPrincipal principal: OAuth2User?): ResponseEntity<DashboardInsights> {
        val userId = extractUserId(principal) ?: return ResponseEntity.status(401).build()

        val today = LocalDate.now()
        val weekStart = today.with(DayOfWeek.MONDAY)

        val weeklySummary = toolService.getPeriodSummary(userId, weekStart, today)

        val trend = toolService.analyzeTrend(userId, mapOf(
            "from" to today.minusWeeks(4).toString(),
            "to" to today.toString(),
            "metric" to "TOTAL_DISTANCE_M"
        ))

        val anomalies = toolService.getAnomalyRuns(userId, today.minusDays(30), today)

        val performance = toolService.analyzePerformance(userId, mapOf(
            "from" to today.minusDays(30).toString(),
            "to" to today.toString()
        ))

        return ResponseEntity.ok(DashboardInsights(
            weeklySummary = weeklySummary,
            trend = trend,
            anomaliesCount = anomalies.size,
            riskScore = performance.riskScore ?: 0,
            riskLevel = when {
                (performance.riskScore ?: 0) >= 60 -> "HIGH"
                (performance.riskScore ?: 0) >= 30 -> "MEDIUM"
                else -> "LOW"
            }
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
