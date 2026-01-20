package io.hansu.pacer.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

/**
 * MCP 엔드포인트용 AuthenticationEntryPoint
 *
 * RFC 9728에 따라 WWW-Authenticate 헤더에 resource_metadata URL을 포함하여
 * MCP 클라이언트가 인가 서버를 찾을 수 있도록 함
 */
@Component
class McpAuthenticationEntryPoint(
    @Value("\${app.base-url:\${oauth.issuer}}") private val appBaseUrl: String
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        val resourceMetadataUrl = "$appBaseUrl/.well-known/oauth-protected-resource"

        // RFC 9728: WWW-Authenticate 헤더에 resource_metadata 포함
        response.setHeader(
            "WWW-Authenticate",
            """Bearer resource_metadata="$resourceMetadataUrl""""
        )
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json"
        response.writer.write("""{"error": "unauthorized", "message": "Authentication required"}""")
    }
}
