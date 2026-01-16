package io.hansu.pacer.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.ai.mcp.server.transport.WebMvcSseServerTransport
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.ServerResponse

@Configuration
class McpServerConfig {

    @Bean
    fun mcpServerTransport(objectMapper: ObjectMapper): WebMvcSseServerTransport {
        return WebMvcSseServerTransport(objectMapper, "/mcp/message")
    }

    @Bean
    fun mcpRouterFunction(transport: WebMvcSseServerTransport): RouterFunction<ServerResponse> {
        return transport.routerFunction
    }
}
