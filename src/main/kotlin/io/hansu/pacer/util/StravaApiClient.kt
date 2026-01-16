package io.hansu.pacer.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.hansu.pacer.config.StravaProps
import io.hansu.pacer.service.StravaAuthService
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class StravaApiClient(
    private val restClient: RestClient,
    private val props: StravaProps,
    private val auth: StravaAuthService
) {
    private val om = jacksonObjectMapper()
    fun getActivity(activityId: Long): JsonNode =
        get("/activities/$activityId")

    fun getActivityLaps(activityId: Long): JsonNode =
        get("/activities/$activityId/laps")

    fun getActivityStreams(activityId: Long, keys: List<String>): JsonNode {
        val keyParam = keys.joinToString(",")
        return get("/activities/$activityId/streams?keys=$keyParam&key_by_type=true")
    }

    fun getAthleteActivities(
        after: Long? = null,
        before: Long? = null,
        page: Int = 1,
        perPage: Int = 30
    ): JsonNode {
        val sb = StringBuilder("/athlete/activities?page=$page&per_page=$perPage")
        if (after != null) sb.append("&after=$after")
        if (before != null) sb.append("&before=$before")
        return get(sb.toString())
    }

    private fun get(path: String): JsonNode {
        val token = auth.getValidAccessToken()
        val body = restClient.get()
            .uri("${props.apiBaseUrl}$path")
            .header("Authorization", "Bearer $token")
            .retrieve()
            .body(String::class.java)
            ?: error("Strava API response is null: $path")
        return om.readTree(body)
    }
}
