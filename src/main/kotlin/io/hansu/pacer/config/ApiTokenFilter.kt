package io.hansu.pacer.config

import io.hansu.pacer.service.ApiTokenService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class ApiTokenFilter(
    private val apiTokenService: ApiTokenService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI
        // MCP 관련 경로에 대해서만 필터 적용 (SSE, 메시지 엔드포인트)
        if (!path.startsWith("/sse") && !path.startsWith("/mcp/")) {
            filterChain.doFilter(request, response)
            return
        }

        if (SecurityContextHolder.getContext().authentication == null) {
            val header = request.getHeader("Authorization")
            if (header != null && header.startsWith("Bearer ")) {
                val token = header.substring(7)
                if (token.startsWith("sk-running-")) {
                    val user = apiTokenService.verifyToken(token)

                    if (user != null) {
                        val attributes = mapOf(
                            "userId" to user.id,
                            "sub" to user.providerId,
                            "email" to user.email
                        )

                        val principal = DefaultOAuth2User(
                            listOf(SimpleGrantedAuthority("ROLE_USER")),
                            attributes,
                            "sub"
                        )

                        val auth = UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.authorities
                        )

                        SecurityContextHolder.getContext().authentication = auth
                    }
                }
            }
        }

        filterChain.doFilter(request, response)
    }
}
