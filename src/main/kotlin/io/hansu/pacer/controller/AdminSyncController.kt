package io.hansu.pacer.controller

import io.hansu.pacer.domain.strava.repository.StravaUserLinksRepository
import io.hansu.pacer.service.job.JobProducer
import io.hansu.pacer.util.StravaApiClient
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin")
class AdminSyncController(
    private val stravaApiClient: StravaApiClient,
    private val stravaUserLinksRepo: StravaUserLinksRepository,
    private val jobProducer: JobProducer
) {
    @PostMapping("/sync/activities")
    fun syncActivities(
        @RequestParam(required = false) from: String?,
        @RequestParam(required = false) to: String?,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "30") perPage: Int,
        @AuthenticationPrincipal principal: OAuth2User?
    ): ResponseEntity<Map<String, Any>> {
        val userId = extractUserId(principal) ?: return ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))

        val links = stravaUserLinksRepo.findByUserId(userId)
        val link = links.firstOrNull()
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "No linked Strava user found for current user"))

        val after = from?.let { java.time.LocalDate.parse(it).atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) }
        val before = to?.let { java.time.LocalDate.parse(it).plusDays(1).atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) }

        val activities = stravaApiClient.getAthleteActivities(userId, after, before, page, perPage)
        
        if (!activities.isArray) {
            return ResponseEntity.ok(mapOf("enqueuedCount" to 0, "message" to "No activities found or invalid response"))
        }

        var count = 0
        activities.forEach { activity ->
            val activityId = activity["id"].asLong()
            jobProducer.enqueueActivityIngestJob(link.userId, activityId)
            count++
        }

        return ResponseEntity.ok(
            mapOf(
                "enqueuedCount" to count,
                "message" to "Enqueued $count activities for ingestion (from=${from ?: "all"}, to=${to ?: "now"})"
            )
        )
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
