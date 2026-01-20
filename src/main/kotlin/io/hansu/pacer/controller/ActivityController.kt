package io.hansu.pacer.controller

import io.hansu.pacer.dto.ActivityDetail
import io.hansu.pacer.service.RunningQueryService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/activities")
class ActivityController(
    private val queryService: RunningQueryService
) {

    @GetMapping("/{id}")
    fun getActivityDetail(
        @PathVariable id: Long,
        @AuthenticationPrincipal principal: OAuth2User?
    ): ResponseEntity<ActivityDetail> {
        val userId = extractUserId(principal) ?: return ResponseEntity.status(401).build()
        val detail = queryService.getActivityDetail(userId, id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(detail)
    }

    @GetMapping("/{id}/streams")
    fun getActivityStreams(
        @PathVariable id: Long,
        @AuthenticationPrincipal principal: OAuth2User?
    ): ResponseEntity<String> {
        val userId = extractUserId(principal) ?: return ResponseEntity.status(401).build()
        val streams = queryService.getActivityStreams(userId, id) ?: return ResponseEntity.notFound().build()
        // streams is already JSON string
        return ResponseEntity.ok()
            .header("Content-Type", "application/json")
            .body(streams)
    }

    private fun extractUserId(principal: OAuth2User?): Long? {
        val raw = principal?.attributes?.get("userId") ?: return null
        return when (raw) {
            is Number -> raw.toLong()
            is String -> raw.toLongOrNull()
            else -> null
        }
    }
}
