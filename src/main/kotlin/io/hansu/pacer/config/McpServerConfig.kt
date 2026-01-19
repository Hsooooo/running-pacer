package io.hansu.pacer.config

import io.hansu.pacer.mcp.prompts.RunningMcpPrompts
import io.hansu.pacer.mcp.resources.RunningMcpResources
import io.hansu.pacer.mcp.tools.RunningInsightTool
import io.hansu.pacer.mcp.tools.RunningQueryTool
import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class McpServerConfig(
    private val queryTool: RunningQueryTool,
    private val insightTool: RunningInsightTool,
    private val resources: RunningMcpResources,
    private val prompts: RunningMcpPrompts
) {
    @Bean
    fun mcpTools(): List<SyncToolSpecification> = listOf(
        queryTool.specification(),
        insightTool.specification()
    )

    @Bean
    fun mcpResources(): List<SyncResourceSpecification> = resources.specifications()

    @Bean
    fun mcpPrompts(): List<SyncPromptSpecification> = prompts.specifications()
}
