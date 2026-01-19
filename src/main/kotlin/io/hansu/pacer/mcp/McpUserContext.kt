package io.hansu.pacer.mcp

import io.hansu.pacer.service.ApiTokenService
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Component
class McpUserContext(
    private val apiTokenService: ApiTokenService
) {
    fun getCurrentUserId(): Long {
        val authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().authentication
        if (authentication != null && authentication.isAuthenticated) {
            val principal = authentication.principal
            if (principal is org.springframework.security.oauth2.jwt.Jwt) {
                val claim = principal.getClaim<Any>("user_id") ?: principal.getClaim<Any>("userId")
                return when (claim) {
                    is Number -> claim.toLong()
                    is String -> claim.toLongOrNull() ?: throw SecurityException("Invalid user_id claim")
                    else -> throw SecurityException("Invalid user_id claim type")
                }
            }
            if (principal is org.springframework.security.oauth2.core.user.OAuth2User) {
                val claim = principal.attributes["userId"]
                return when (claim) {
                    is Number -> claim.toLong()
                    is String -> claim.toLongOrNull() ?: throw SecurityException("Invalid userId claim")
                    else -> throw SecurityException("Invalid userId claim type")
                }
            }
        }

        val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
            ?: throw IllegalStateException("No active request found")

        val authHeader = request.getHeader("Authorization")
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7)
            if (token.startsWith("sk-running-")) {
                val user = apiTokenService.verifyToken(token)
                if (user != null) {
                    return user.id
                }
            }
        }

        throw SecurityException("Invalid or missing authentication")
    }
}
