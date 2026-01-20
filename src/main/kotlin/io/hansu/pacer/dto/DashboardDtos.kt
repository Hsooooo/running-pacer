package io.hansu.pacer.dto

data class DashboardInsights(
    val weeklySummary: PeriodSummary,
    val trend: TrendAnalysisResult,
    val anomaliesCount: Int,
    val riskScore: Int,
    val riskLevel: String
)
