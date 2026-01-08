package io.hansu.pacer.domain.activity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "activity_streams", indexes = [Index(name = "idx_stream_user", columnList = "user_id")])
class ActivityStreamsEntity(
    @Id
    @Column(name = "activity_id")
    val activityId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "streams_json", columnDefinition = "json", nullable = false)
    val streamsJson: String,

    @Column(name = "stream_keys", nullable = false, length = 255)
    val streamKeys: String,

    @Column(name = "sample_count")
    val sampleCount: Int? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)