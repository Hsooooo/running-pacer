package io.hansu.pacer.mcp.resources

import com.fasterxml.jackson.databind.ObjectMapper
import io.hansu.pacer.mcp.McpUserContext
import io.hansu.pacer.service.RunningQueryService
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification
import io.modelcontextprotocol.spec.McpSchema
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class RunningMcpResources(
    private val queryService: RunningQueryService,
    private val userContext: McpUserContext,
    private val objectMapper: ObjectMapper
) {
    fun specifications(): List<SyncResourceSpecification> = listOf(
        recentActivitiesResource(),
        currentWeekSummaryResource(),
        currentMonthSummaryResource()
    )

    private fun recentActivitiesResource() = SyncResourceSpecification(
        McpSchema.Resource(
            "running://activities/recent",
            "Recent Activities",
            "최근 30일간의 러닝 활동 목록",
            "application/json",
            null
        )
    ) { _, _ ->
        val userId = userContext.getCurrentUserId()
        val to = LocalDate.now()
        val from = to.minusDays(30)
        val activities = queryService.listActivities(userId, from, to, 30)
        McpSchema.ReadResourceResult(
            listOf(
                McpSchema.TextResourceContents(
                    "running://activities/recent",
                    "application/json",
                    objectMapper.writeValueAsString(activities)
                )
            )
        )
    }

    private fun currentWeekSummaryResource() = SyncResourceSpecification(
        McpSchema.Resource(
            "running://summary/week",
            "Current Week Summary",
            "이번 주 러닝 요약 통계",
            "application/json",
            null
        )
    ) { _, _ ->
        val userId = userContext.getCurrentUserId()
        val today = LocalDate.now()
        val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val summary = queryService.getPeriodSummary(userId, weekStart, today)
        McpSchema.ReadResourceResult(
            listOf(
                McpSchema.TextResourceContents(
                    "running://summary/week",
                    "application/json",
                    objectMapper.writeValueAsString(summary)
                )
            )
        )
    }

    private fun currentMonthSummaryResource() = SyncResourceSpecification(
        McpSchema.Resource(
            "running://summary/month",
            "Current Month Summary",
            "이번 달 러닝 요약 통계",
            "application/json",
            null
        )
    ) { _, _ ->
        val userId = userContext.getCurrentUserId()
        val today = LocalDate.now()
        val monthStart = today.withDayOfMonth(1)
        val summary = queryService.getPeriodSummary(userId, monthStart, today)
        McpSchema.ReadResourceResult(
            listOf(
                McpSchema.TextResourceContents(
                    "running://summary/month",
                    "application/json",
                    objectMapper.writeValueAsString(summary)
                )
            )
        )
    }
}
