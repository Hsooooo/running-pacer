package io.hansu.pacer.service

import io.hansu.pacer.config.StravaProps
import io.hansu.pacer.domain.strava.StravaUserLinksEntity
import io.hansu.pacer.domain.strava.repository.StravaTokenRepository
import io.hansu.pacer.domain.strava.repository.StravaUserLinksRepository
import io.hansu.pacer.domain.user.UserEntity
import io.hansu.pacer.domain.user.repository.UserRepository
import io.hansu.pacer.dto.StravaTokenResponse
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClient
import java.time.Instant

@Service
class StravaAuthService(
    private val restClient: RestClient,
    private val props: StravaProps,
    private val tokenRepo: StravaTokenRepository,
    private val stravaUserLinksRepo: StravaUserLinksRepository,
    private val userRepo: UserRepository
) {
    fun exchangeCodeAndSaveTokens(code: String) {
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

        val userId = stravaUserLinksRepo.findByAthleteId(athleteId)?.userId ?: run {
            val newUser = userRepo.save(UserEntity(nickname = "Strava Athlete $athleteId"))
            stravaUserLinksRepo.save(
                StravaUserLinksEntity(
                    userId = newUser.id,
                    athleteId = athleteId,
                    scope = null,
                    status = "ACTIVE"
                )
            )
            newUser.id
        }

        tokenRepo.upsert(
            userId = userId,
            athleteId = athleteId,
            accessToken = res.access_token,
            refreshToken = res.refresh_token,
            expiresAt = res.expires_at,
            scope = null
        )
    }

    /** 항상 유효한 access token을 반환 */
    fun getValidAccessToken(): String {
        val athleteId = stravaUserLinksRepo.findAll().firstOrNull()?.athleteId
            ?: error("No Strava user linked. Do OAuth first.")
        val userId = stravaUserLinksRepo.findByAthleteId(athleteId)?.userId
            ?: error("User not found for athlete $athleteId")

        val row = tokenRepo.find(userId) ?: error("Strava token not found. Do OAuth first.")
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
