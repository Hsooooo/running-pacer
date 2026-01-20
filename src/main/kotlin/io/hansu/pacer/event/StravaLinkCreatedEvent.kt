package io.hansu.pacer.event

data class StravaLinkCreatedEvent(
    val userId: Long,
    val athleteId: Long
)
