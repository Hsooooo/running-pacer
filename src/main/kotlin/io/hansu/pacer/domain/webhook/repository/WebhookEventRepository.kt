package io.hansu.pacer.domain.webhook.repository

import io.hansu.pacer.domain.webhook.WebhookEventEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WebhookEventRepository : JpaRepository<WebhookEventEntity, Long> {
    fun findByObjectId(objectId: Long): List<WebhookEventEntity>
    fun findByOwnerIdAndObjectId(ownerId: Long, objectId: Long): List<WebhookEventEntity>
}
