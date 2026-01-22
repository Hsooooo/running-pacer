package io.hansu.pacer.dto

import io.hansu.pacer.domain.training.WorkoutType
import io.hansu.pacer.service.BestEffort

data class DashboardInsights(
        val weeklySummary: PeriodSummary,
        val trend: TrendAnalysisResult,
        val anomaliesCount: Int,
        val riskScore: Int,
        val riskLevel: String,
        // New training metrics
        val currentVdot: Double? = null,
        val bestEfforts: List<BestEffort>? = null,
        val fitnessStatus: FitnessStatus? = null,
        val workoutDistribution: WorkoutDistributionDto? = null,
        val trainingPaces: Map<String, String>? = null // "EASY" -> "5:30/km"
)

data class WorkoutDistributionDto(
        val totalRuns: Int,
        val counts: Map<WorkoutType, Int>,
        val percentages: Map<WorkoutType, Double>,
        val balanceScore: Int,
        val balanceAssessment: String,
        val easyHardRatio: String,
        val recommendations: List<String>
)
