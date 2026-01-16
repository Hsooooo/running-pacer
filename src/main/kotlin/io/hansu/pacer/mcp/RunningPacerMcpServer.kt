package io.hansu.pacer.mcp

import io.hansu.pacer.mcp.prompts.RunningMcpPrompts
import io.hansu.pacer.mcp.resources.RunningMcpResources
import io.hansu.pacer.mcp.tools.RunningInsightTool
import io.hansu.pacer.mcp.tools.RunningQueryTool
import org.springframework.ai.mcp.server.McpServer
import org.springframework.ai.mcp.server.McpSyncServer
import org.springframework.ai.mcp.server.transport.WebMvcSseServerTransport
import org.springframework.ai.mcp.spec.McpSchema.ServerCapabilities
import org.springframework.stereotype.Component
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy

@Component
class RunningPacerMcpServer(
    private val transport: WebMvcSseServerTransport,
    private val queryTool: RunningQueryTool,
    private val insightTool: RunningInsightTool,
    private val resources: RunningMcpResources,
    private val prompts: RunningMcpPrompts
) {
    private lateinit var server: McpSyncServer

    @PostConstruct
    fun initialize() {
        server = McpServer.sync(transport)
            .serverInfo("running-pacer", "1.1.0")
            .capabilities(
                ServerCapabilities.builder()
                    .tools(true)
                    .resources(false, true)
                    .prompts(true)
                    .logging()
                    .build()
            )
            .tools(
                queryTool.registration(),
                insightTool.registration()
            )
            .resources(resources.registrations())
            .prompts(prompts.registrations())
            .build()
    }

    @PreDestroy
    fun shutdown() {
        server.closeGracefully()
    }
}
