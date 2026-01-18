package io.hansu.pacer.domain.strava.repository

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class StravaTokenRepository(
    private val jdbc: NamedParameterJdbcTemplate
) {
    fun upsert(
        userId: Long,
        athleteId: Long?,
        accessToken: String,
        refreshToken: String,
        expiresAt: Long,
        scope: String?
    ) {
        val sql = """
            insert into strava_tokens (user_id, athlete_id, access_token, refresh_token, expires_at, scope)
            values (:userId, :athleteId, :accessToken, :refreshToken, :expiresAt, :scope)
            on conflict (user_id) do update set
              athlete_id = excluded.athlete_id,
              access_token = excluded.access_token,
              refresh_token = excluded.refresh_token,
              expires_at = excluded.expires_at,
              scope = excluded.scope,
              updated_at = current_timestamp
        """.trimIndent()

        jdbc.update(sql, mapOf(
            "userId" to userId,
            "athleteId" to athleteId,
            "accessToken" to accessToken,
            "refreshToken" to refreshToken,
            "expiresAt" to expiresAt,
            "scope" to scope
        ))
    }

    fun find(userId: Long): StravaTokenRow? {
        val sql = "select * from strava_tokens where user_id = :userId"
        val rows = jdbc.query(sql, mapOf("userId" to userId)) { rs, _ ->
            StravaTokenRow(
                userId = rs.getLong("user_id"),
                athleteId = rs.getLong("athlete_id").takeIf { !rs.wasNull() },
                accessToken = rs.getString("access_token"),
                refreshToken = rs.getString("refresh_token"),
                expiresAt = rs.getLong("expires_at"),
                scope = rs.getString("scope")
            )
        }
        return rows.firstOrNull()
    }
}

data class StravaTokenRow(
    val userId: Long,
    val athleteId: Long?,
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val scope: String?
)
