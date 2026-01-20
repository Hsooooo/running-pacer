package io.hansu.pacer.service

import io.hansu.pacer.config.StravaProps
import io.hansu.pacer.domain.strava.StravaUserLinksEntity
import io.hansu.pacer.domain.strava.repository.StravaTokenRepository
import io.hansu.pacer.domain.strava.repository.StravaUserLinksRepository
import io.hansu.pacer.dto.StravaTokenResponse
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClient
import io.hansu.pacer.event.StravaLinkCreatedEvent
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant

@Service
class StravaAuthService(
    private val restClient: RestClient,
    private val props: StravaProps,
    private val tokenRepo: StravaTokenRepository,
    private val stravaUserLinksRepo: StravaUserLinksRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun exchangeCodeAndSaveTokens(code: String, targetUserId: Long) {
        val body: MultiValueMap<String, String> = LinkedMultiValueMap<String, String>().apply {
            add("client_id", props.clientId.toString())
            add("client_secret", props.clientSecret)
            add("code", code)
            add("grant_type", "authorization_code")
        }

        val res = restClient.post()
            .uri("${props.baseUrl}/oauth/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(body)
            .retrieve()
            .body(StravaTokenResponse::class.java)
            ?: error("Strava token response is null")

        val athleteId = (res.athlete?.get("id") as? Number)?.toLong() ?: error("Athlete ID not found in Strava response")

        val existingLink = stravaUserLinksRepo.findByAthleteId(athleteId)
        
        if (existingLink != null) {
            if (existingLink.userId != targetUserId) {
                logger.warn("Strava athlete $athleteId was linked to user ${existingLink.userId}, moving to $targetUserId")
                stravaUserLinksRepo.delete(existingLink)
                stravaUserLinksRepo.flush()
                saveNewLink(targetUserId, athleteId)
            }
        } else {
            saveNewLink(targetUserId, athleteId)
        }

        tokenRepo.upsert(
            userId = targetUserId,
            athleteId = athleteId,
            accessToken = res.access_token,
            refreshToken = res.refresh_token,
            expiresAt = res.expires_at,
            scope = null
        )

        eventPublisher.publishEvent(StravaLinkCreatedEvent(targetUserId, athleteId))
    }

    private fun saveNewLink(userId: Long, athleteId: Long) {
        stravaUserLinksRepo.save(
            StravaUserLinksEntity(
                userId = userId,
                athleteId = athleteId,
                scope = null,
                status = "ACTIVE"
            )
        )
    }

    fun getValidAccessToken(userId: Long): String {
        val links = stravaUserLinksRepo.findByUserId(userId)
        if (links.isEmpty()) {
            error("No Strava account linked for user $userId")
        }
        val link = links.first()
        val athleteId = link.athleteId

        val row = tokenRepo.find(userId) ?: error("Strava token not found for user $userId")
        val now = Instant.now().epochSecond

        return if (row.expiresAt <= now + 300) {
            refreshAndGet(row.refreshToken, userId, athleteId)
        } else {
            row.accessToken
        }
    }

    private fun refreshAndGet(refreshToken: String, userId: Long, athleteId: Long): String {
        val body = LinkedMultiValueMap<String, String>().apply {
            add("client_id", props.clientId)
            add("client_secret", props.clientSecret)
            add("grant_type", "refresh_token")
            add("refresh_token", refreshToken)
        }

        val res = restClient.post()
            .uri("${props.baseUrl}/oauth/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(body)
            .retrieve()
            .body(StravaTokenResponse::class.java)
            ?: error("Strava refresh response is null")

        val refreshedAthleteId = (res.athlete?.get("id") as? Number)?.toLong()

        tokenRepo.upsert(
            userId = userId,
            athleteId = refreshedAthleteId ?: athleteId,
            accessToken = res.access_token,
            refreshToken = res.refresh_token,
            expiresAt = res.expires_at,
            scope = null
        )
        return res.access_token
    }
}
