package io.hansu.pacer.config

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Protected Resource Metadata (RFC 9728) 필터
 *
 * Spring Security의 기본 메타데이터 응답을 재정의하여
 * MCP 스펙에서 요구하는 authorization_servers 필드를 포함
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class ProtectedResourceMetadataFilter(
    @Value("\${oauth.issuer}") private val issuer: String,
    @Value("\${app.base-url:\${oauth.issuer}}") private val appBaseUrl: String,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (request.requestURI == "/.well-known/oauth-protected-resource") {
            val metadata = mapOf(
                "resource" to appBaseUrl,
                "authorization_servers" to listOf(issuer),
                "bearer_methods_supported" to listOf("header"),
                "scopes_supported" to listOf("openid", "offline_access", "mcp")
            )

            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.characterEncoding = "UTF-8"
            response.writer.write(objectMapper.writeValueAsString(metadata))
            return
        }

        filterChain.doFilter(request, response)
    }
}
