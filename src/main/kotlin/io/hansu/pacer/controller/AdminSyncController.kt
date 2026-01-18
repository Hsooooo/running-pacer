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
        @RequestParam(required = false) after: Long?,
        @RequestParam(required = false) before: Long?,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "30") perPage: Int,
        @AuthenticationPrincipal principal: OAuth2User?
    ): ResponseEntity<Map<String, Any>> {
        val userId = principal?.attributes?.get("userId") as? Long 
            ?: return ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))

        val links = stravaUserLinksRepo.findByUserId(userId)
        val link = links.firstOrNull()
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "No linked Strava user found for current user"))

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
                "message" to "Enqueued $count activities for ingestion (page=$page, perPage=$perPage)"
            )
        )
    }
}
