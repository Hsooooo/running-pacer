package io.hansu.pacer.mcp.tools

import com.fasterxml.jackson.databind.ObjectMapper
import io.hansu.pacer.mcp.McpUserContext
import io.hansu.pacer.service.RunningToolService
import org.springframework.ai.mcp.server.McpServerFeatures.SyncToolRegistration
import org.springframework.ai.mcp.spec.McpSchema.CallToolResult
import org.springframework.ai.mcp.spec.McpSchema.TextContent
import org.springframework.ai.mcp.spec.McpSchema.Tool
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class RunningQueryTool(
    private val toolService: RunningToolService,
    private val userContext: McpUserContext,
    private val objectMapper: ObjectMapper
) {
    fun registration(): SyncToolRegistration {
        return SyncToolRegistration(
            Tool(
                "running_query",
                "러닝 데이터 조회를 위한 통합 도구입니다. 활동 목록, 기간 요약, 이상치 분석 등을 수행합니다.",
                """
                {
                  "type": "object",
                  "properties": {
                    "mode": {
                      "type": "string",
                      "enum": ["activities", "summary", "anomalies"],
                      "description": "조회 모드 (activities: 활동 목록, summary: 기간 요약, anomalies: 이상치 조회)"
                    },
                    "from": {"type": "string", "format": "date", "description": "시작일 (YYYY-MM-DD)"},
                    "to": {"type": "string", "format": "date", "description": "종료일 (YYYY-MM-DD)"},
                    "limit": {"type": "integer", "default": 30}
                  },
                  "required": ["mode", "from", "to"]
                }
                """.trimIndent()
            )
        ) { args ->
            val userId = userContext.getCurrentUserId()
            val mode = args["mode"] as String
            val from = LocalDate.parse(args["from"] as String)
            val to = LocalDate.parse(args["to"] as String)
            val limit = (args["limit"] as? Number)?.toInt() ?: 30

            val result = when (mode) {
                "activities" -> toolService.listActivities(userId, from, to, limit)
                "summary" -> toolService.getPeriodSummary(userId, from, to)
                "anomalies" -> toolService.getAnomalyRuns(userId, from, to)
                else -> throw IllegalArgumentException("Unknown mode: $mode")
            }

            CallToolResult(listOf(TextContent(objectMapper.writeValueAsString(result))), false)
        }
    }
}
