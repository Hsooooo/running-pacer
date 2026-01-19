package io.hansu.pacer.mcp.tools

import com.fasterxml.jackson.databind.ObjectMapper
import io.hansu.pacer.mcp.McpUserContext
import io.hansu.pacer.service.RunningToolService
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification
import io.modelcontextprotocol.spec.McpSchema
import org.springframework.stereotype.Component

@Component
class RunningInsightTool(
    private val toolService: RunningToolService,
    private val userContext: McpUserContext,
    private val objectMapper: ObjectMapper
) {
    fun specification(): SyncToolSpecification {
        val inputSchema = McpSchema.JsonSchema(
            "object",
            mapOf(
                "mode" to mapOf(
                    "type" to "string",
                    "enum" to listOf("compare", "trend", "performance", "coaching", "set_goal"),
                    "description" to "분석 모드 (compare: 비교, trend: 추세, performance: 성과분석, coaching: 코칭조언, set_goal: 목표설정)"
                ),
                "params" to mapOf(
                    "type" to "object",
                    "description" to "모드별 필요한 파라미터 (예: 비교 시 period_a_from 등, 목표설정 시 race_name 등)",
                    "additionalProperties" to true
                )
            ),
            listOf("mode"),
            true,
            null,
            null
        )
        
        val tool = McpSchema.Tool.builder()
            .name("running_insight")
            .description("러닝 데이터 분석 및 코칭을 위한 통합 도구입니다. 성과 비교, 추세 분석, 목표 설정 및 코칭을 제공합니다.")
            .inputSchema(inputSchema)
            .build()
        
        return SyncToolSpecification(
            tool
        ) { _, args ->
            val userId = userContext.getCurrentUserId()
            val mode = args["mode"] as String
            
            @Suppress("UNCHECKED_CAST")
            val params = (args["params"] as? Map<String, Any>) ?: emptyMap()

            val result: Any = when (mode) {
                "compare" -> toolService.comparePeriods(userId, params)
                "trend" -> toolService.analyzeTrend(userId, params)
                "performance" -> toolService.analyzePerformance(userId, params)
                "coaching" -> toolService.getCoachingAdvice(userId)
                "set_goal" -> toolService.setRaceGoal(userId, params)
                else -> throw IllegalArgumentException("Unknown mode: $mode")
            }

            McpSchema.CallToolResult(
                listOf(McpSchema.TextContent(objectMapper.writeValueAsString(result))),
                false
            )
        }
    }
}
