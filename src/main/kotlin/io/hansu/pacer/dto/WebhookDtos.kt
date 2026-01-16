package io.hansu.pacer.dto

data class StravaWebhookEvent(
    val object_type: String,
    val aspect_type: String,
    val object_id: Long,
    val owner_id: Long,
    val event_time: Long,
    val subscription_id: Long? = null
)

data class StravaWebhookResponse(
    val status: String,
    val message: String
)
