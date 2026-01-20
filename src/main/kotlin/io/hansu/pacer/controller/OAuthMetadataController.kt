package io.hansu.pacer.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * MCP OAuth 스펙에서 요구하는 메타데이터 엔드포인트 제공
 * - Protected Resource Metadata (RFC 9728)
 */
@RestController
class OAuthMetadataController(
    @Value("\${oauth.issuer}") private val issuer: String,
    @Value("\${app.base-url:\${oauth.issuer}}") private val appBaseUrl: String
) {

    /**
     * Protected Resource Metadata (RFC 9728)
     * MCP 클라이언트가 인가 서버를 찾기 위해 사용
     */
    @GetMapping(
        "/.well-known/oauth-protected-resource",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun protectedResourceMetadata(): Map<String, Any> {
        return mapOf(
            // 리소스 식별자 (MCP 서버 URL)
            "resource" to appBaseUrl,
            // 인가 서버 목록
            "authorization_servers" to listOf(issuer),
            // 지원하는 토큰 타입
            "bearer_methods_supported" to listOf("header"),
            // 지원하는 scope
            "scopes_supported" to listOf("openid", "offline_access", "mcp"),
            // 리소스 문서 (선택)
            "resource_documentation" to "$appBaseUrl/docs"
        )
    }
}
