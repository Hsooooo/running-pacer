package io.hansu.pacer.controller

import io.hansu.pacer.domain.auth.ApiTokenEntity
import io.hansu.pacer.service.ApiTokenService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/tokens")
class ApiTokenController(
    private val apiTokenService: ApiTokenService
) {

    @PostMapping
    fun createToken(
        @RequestBody request: CreateTokenRequest,
        @AuthenticationPrincipal principal: OAuth2User?
    ): ResponseEntity<ApiTokenResponse> {
        if (principal == null) return ResponseEntity.status(401).build()
        
        val userId = (principal.attributes["userId"] as? String)?.toLongOrNull()
            ?: return ResponseEntity.status(401).build()
        
        val token = apiTokenService.createToken(userId, request.name)
        return ResponseEntity.ok(ApiTokenResponse.from(token))
    }

    @GetMapping
    fun listTokens(@AuthenticationPrincipal principal: OAuth2User?): ResponseEntity<List<ApiTokenResponse>> {
        if (principal == null) return ResponseEntity.status(401).build()
        val userId = (principal.attributes["userId"] as? String)?.toLongOrNull()
             ?: return ResponseEntity.status(401).build()
        
        val tokens = apiTokenService.listTokens(userId)
        return ResponseEntity.ok(tokens.map { ApiTokenResponse.from(it) })
    }
    
    @DeleteMapping("/{tokenId}")
    fun deleteToken(
        @PathVariable tokenId: Long,
        @AuthenticationPrincipal principal: OAuth2User?
    ): ResponseEntity<Void> {
        if (principal == null) return ResponseEntity.status(401).build()
        val userId = (principal.attributes["userId"] as? String)?.toLongOrNull()
             ?: return ResponseEntity.status(401).build()
        
        apiTokenService.deleteToken(userId, tokenId)
        return ResponseEntity.noContent().build()
    }
}

data class CreateTokenRequest(val name: String)

data class ApiTokenResponse(
    val id: Long,
    val name: String,
    val token: String, 
    val createdAt: String,
    val lastUsedAt: String?
) {
    companion object {
        fun from(entity: ApiTokenEntity) = ApiTokenResponse(
            id = entity.id,
            name = entity.name,
            token = entity.token,
            createdAt = entity.createdAt.toString(),
            lastUsedAt = entity.lastUsedAt?.toString()
        )
    }
}
