package io.hansu.pacer.domain.webhook

import jakarta.persistence.*

@Entity
@Table(
    name = "webhook_events",
    indexes = [
        Index(name = "idx_webhook_object_id", columnList = "object_id"),
        Index(name = "idx_webhook_event_time", columnList = "event_time")
    ]
)
class WebhookEventEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    val id: Long = 0,

    @Column(name = "object_type", nullable = false, length = 20)
    val objectType: String,

    @Column(name = "aspect_type", nullable = false, length = 20)
    val aspectType: String,

    @Column(name = "object_id", nullable = false)
    val objectId: Long,

    @Column(name = "owner_id", nullable = false)
    val ownerId: Long,

    @Column(name = "event_time", nullable = false)
    val eventTime: Long,

    @Column(name = "event_json", columnDefinition = "JSON", nullable = false)
    val eventJson: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
) {
    init {
        require(objectType == "activity") { "Only activity events are supported currently" }
    }
}
