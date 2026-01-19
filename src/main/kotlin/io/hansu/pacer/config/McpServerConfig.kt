package io.hansu.pacer.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.ai.mcp.server.transport.WebMvcSseServerTransport
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.ServerResponse

@Configuration
class McpServerConfig {

    private val logger = LoggerFactory.getLogger(McpServerConfig::class.java)

    @Bean
    fun mcpServerTransport(objectMapper: ObjectMapper): WebMvcSseServerTransport {
        return WebMvcSseServerTransport(objectMapper, "/mcp/message")
    }

    @Bean
    fun mcpRouterFunction(transport: WebMvcSseServerTransport): RouterFunction<ServerResponse> {
        return RouterFunction { request ->
            val path = request.path()
            if (path == "/sse" || path == "/mcp/message") {
                logger.info("MCP Request: {} {}, remote={}", request.method(), path, request.remoteAddress())
                request.headers().asHttpHeaders().forEach { name, values ->
                    logger.debug("Header {}: {}", name, values)
                }
            }
            transport.routerFunction.route(request)
        }
    }
}
