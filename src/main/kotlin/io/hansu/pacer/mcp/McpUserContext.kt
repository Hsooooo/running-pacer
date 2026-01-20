package io.hansu.pacer.mcp

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

@Component
class McpUserContext {

    /**
     * 현재 인증된 사용자의 ID를 반환합니다.
     *
     * 지원하는 인증 방식:
     * 1. JWT (OAuth2 Authorization Code Flow) - user_id 클레임에서 추출
     * 2. API Token (Bearer sk-running-xxx) - OAuth2User attributes에서 추출
     */
    fun getCurrentUserId(): Long {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw McpAuthenticationException("인증이 필요합니다")

        val principal = auth.principal

        // JWT 토큰 (OAuth2 Authorization Code Flow)
        if (principal is Jwt) {
            val userId = principal.getClaim<Any>("user_id")
                ?: throw McpAuthenticationException("JWT에 user_id 클레임이 없습니다")
            
            return when (userId) {
                is Number -> userId.toLong()
                is String -> userId.toLongOrNull() ?: throw McpAuthenticationException("JWT user_id 형식이 잘못되었습니다")
                else -> throw McpAuthenticationException("JWT user_id 타입이 잘못되었습니다: ${userId::class.simpleName}")
            }
        }

        // API 토큰 (ApiTokenFilter에서 설정한 OAuth2User)
        if (principal is OAuth2User) {
            val userId = principal.attributes["userId"]
                ?: throw McpAuthenticationException("인증 정보에 userId가 없습니다")
            return when (userId) {
                is Number -> userId.toLong()
                is String -> userId.toLongOrNull()
                    ?: throw McpAuthenticationException("userId 형식이 잘못되었습니다")
                else -> throw McpAuthenticationException("userId 타입이 잘못되었습니다")
            }
        }

        throw McpAuthenticationException("지원하지 않는 인증 방식입니다: ${principal?.let { it::class.simpleName } ?: "null"}")
    }
}

class McpAuthenticationException(message: String) : RuntimeException(message)
