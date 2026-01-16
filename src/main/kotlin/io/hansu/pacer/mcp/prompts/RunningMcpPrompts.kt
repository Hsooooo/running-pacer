package io.hansu.pacer.mcp.prompts

import org.springframework.ai.mcp.server.McpServerFeatures.SyncPromptRegistration
import org.springframework.ai.mcp.spec.McpSchema.GetPromptResult
import org.springframework.ai.mcp.spec.McpSchema.Prompt
import org.springframework.ai.mcp.spec.McpSchema.PromptArgument
import org.springframework.ai.mcp.spec.McpSchema.PromptMessage
import org.springframework.ai.mcp.spec.McpSchema.Role
import org.springframework.ai.mcp.spec.McpSchema.TextContent
import org.springframework.stereotype.Component

@Component
class RunningMcpPrompts {

    fun registrations(): List<SyncPromptRegistration> = listOf(
        weeklyReportPrompt(),
        performanceAnalysisPrompt(),
        trainingAdvicePrompt()
    )

    private fun weeklyReportPrompt() = SyncPromptRegistration(
        Prompt(
            "weekly_report",
            "주간 러닝 리포트를 생성하기 위한 프롬프트 템플릿",
            listOf(
                PromptArgument("week_offset", "몇 주 전 데이터인지 (0=이번주, 1=지난주)", false)
            )
        )
    ) { request ->
        val weekOffset = request.arguments?.get("week_offset")?.toString()?.toIntOrNull() ?: 0
        val weekLabel = when (weekOffset) {
            0 -> "이번 주"
            1 -> "지난 주"
            else -> "${weekOffset}주 전"
        }

        GetPromptResult(
            "$weekLabel 러닝 리포트 생성",
            listOf(
                PromptMessage(
                    Role.USER,
                    TextContent(
                        """
                        당신은 전문 러닝 코치입니다. $weekLabel 러닝 데이터를 분석해주세요.

                        다음 도구들을 사용해서 데이터를 수집하세요:
                        1. get_period_summary - 기간 요약 조회
                        2. list_activities - 개별 활동 목록
                        3. get_anomaly_runs - 이상 패턴 러닝

                        분석 결과는 다음 형식의 JSON으로 반환해주세요:
                        {
                          "summary": "한 문장 요약",
                          "highlights": ["주요 포인트 1", "주요 포인트 2"],
                          "concerns": ["우려 사항"],
                          "next_week_focus": "다음 주 집중 포인트"
                        }
                        """.trimIndent()
                    )
                )
            )
        )
    }

    private fun performanceAnalysisPrompt() = SyncPromptRegistration(
        Prompt(
            "performance_analysis",
            "러닝 성과 분석을 위한 프롬프트 템플릿",
            listOf(
                PromptArgument("days", "분석할 기간 (일)", true)
            )
        )
    ) { request ->
        val days = request.arguments?.get("days")?.toString()?.toIntOrNull() ?: 30

        GetPromptResult(
            "최근 ${days}일 성과 분석",
            listOf(
                PromptMessage(
                    Role.USER,
                    TextContent(
                        """
                        최근 ${days}일간의 러닝 데이터를 종합 분석해주세요.

                        analyze_performance 도구를 사용하여 구조화된 분석 결과를 얻고,
                        그 결과를 바탕으로 다음을 포함한 리포트를 작성해주세요:

                        1. 훈련 볼륨 평가 (적정/부족/과다)
                        2. 페이스 변화 추세
                        3. 피로도 지표 분석
                        4. 구체적인 개선 권장사항

                        결과는 구조화된 JSON 형식으로 반환해주세요.
                        """.trimIndent()
                    )
                )
            )
        )
    }

    private fun trainingAdvicePrompt() = SyncPromptRegistration(
        Prompt(
            "training_advice",
            "훈련 조언을 위한 프롬프트 템플릿",
            listOf(
                PromptArgument("goal", "훈련 목표 (예: 5km 기록 단축, 마라톤 완주)", true)
            )
        )
    ) { request ->
        val goal = request.arguments?.get("goal") ?: "일반적인 체력 향상"

        GetPromptResult(
            "훈련 조언: $goal",
            listOf(
                PromptMessage(
                    Role.USER,
                    TextContent(
                        """
                        사용자의 목표: $goal

                        현재 러닝 데이터를 분석하고, 이 목표를 달성하기 위한
                        구체적인 훈련 조언을 제공해주세요.

                        다음 도구들을 활용하세요:
                        1. analyze_performance - 현재 상태 파악
                        2. get_trend - 최근 추세 확인
                        3. compare_periods - 이전 기간과 비교

                        조언은 다음 JSON 형식으로 반환해주세요:
                        {
                          "current_fitness_level": "초급/중급/고급",
                          "goal_feasibility": "실현 가능성 평가",
                          "weekly_plan": {
                            "easy_runs": "횟수 및 거리",
                            "tempo_runs": "횟수 및 거리",
                            "long_runs": "횟수 및 거리"
                          },
                          "key_advice": ["조언1", "조언2"],
                          "warnings": ["주의사항"]
                        }
                        """.trimIndent()
                    )
                )
            )
        )
    }
}
