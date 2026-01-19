package io.hansu.pacer.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.ai.mcp.server.transport.WebMvcSseServerTransport
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.RouterFunctions
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import java.util.*

@Configuration
class McpServerConfig {

    private val logger = LoggerFactory.getLogger(McpServerConfig::class.java)

    @Bean
    fun mcpServerTransport(objectMapper: ObjectMapper): WebMvcSseServerTransport {
        return WebMvcSseServerTransport(objectMapper, "/mcp", "/mcp")
    }

    @Bean
    fun mcpRouterFunction(transport: WebMvcSseServerTransport): RouterFunction<ServerResponse> {
        return RouterFunctions.route()
            .GET("/sse") { request -> delegateTo(request, "/mcp", transport) }
            .POST("/sse") { request -> delegateTo(request, "/mcp", transport) }
            .POST("/mcp/message") { request -> delegateTo(request, "/mcp", transport) }
            .add(transport.routerFunction)
            .build()
    }

    private fun delegateTo(request: ServerRequest, targetPath: String, transport: WebMvcSseServerTransport): ServerResponse {
        logger.info("MCP Proxy: {} {} -> {}", request.method(), request.path(), targetPath)
        val wrappedRequest = object : ServerRequest by request {
            override fun path(): String = targetPath
            override fun uri(): java.net.URI = java.net.URI.create(targetPath)
        }
        return transport.routerFunction.route(wrappedRequest)
            .map { handler -> handler.handle(wrappedRequest) }
            .orElseGet { ServerResponse.notFound().build() }
    }
}
