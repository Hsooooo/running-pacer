package io.hansu.pacer.domain.raw.repository

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class RawStravaPayloadRepository(
    private val jdbc: NamedParameterJdbcTemplate
) {
    fun saveRaw(userId: Long, payloadType: String, sourceActivityId: Long?, payloadJson: String) {
        val sql = """
            insert into raw_strava_payloads
              (user_id, provider, activity_id, payload_type, payload_json)
            values
              (:userId, 'STRAVA', :sourceActivityId, :payloadType, cast(:payloadJson as jsonb))
        """.trimIndent()

        val params = mapOf(
            "userId" to userId,
            "payloadType" to payloadType,
            "sourceActivityId" to sourceActivityId,
            "payloadJson" to payloadJson
        )
        jdbc.update(sql, params)
    }
}