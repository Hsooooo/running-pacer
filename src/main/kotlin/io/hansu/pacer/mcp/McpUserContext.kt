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
        val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
            ?: throw IllegalStateException("No active request found")

        val authHeader = request.getHeader("Authorization")
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7)
            val user = apiTokenService.verifyToken(token)
            if (user != null) {
                return user.id
            }
        }
        
        throw SecurityException("Invalid or missing API token")
    }
}
