package io.hansu.pacer.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.hansu.pacer.domain.strava.repository.StravaUserLinksRepository
import io.hansu.pacer.domain.webhook.WebhookEventEntity
import io.hansu.pacer.domain.webhook.repository.WebhookEventRepository
import io.hansu.pacer.dto.StravaWebhookEvent
import io.hansu.pacer.service.job.JobProducer
import org.springframework.stereotype.Service

@Service
class WebhookService(
    private val webhookEventRepo: WebhookEventRepository,
    private val stravaUserLinksRepo: StravaUserLinksRepository,
    private val jobProducer: JobProducer
) {
    private val om = jacksonObjectMapper()

    fun processWebhookEvent(event: StravaWebhookEvent, rawJson: String): String {
        val existingEvents = webhookEventRepo.findByOwnerIdAndObjectId(event.owner_id, event.object_id)
        val isDuplicate = existingEvents.any {
            it.objectType == event.object_type &&
            it.aspectType == event.aspect_type &&
            it.eventTime == event.event_time
        }

        if (isDuplicate) {
            return "OK"
        }

        webhookEventRepo.save(
            WebhookEventEntity(
                objectType = event.object_type,
                aspectType = event.aspect_type,
                objectId = event.object_id,
                ownerId = event.owner_id,
                eventTime = event.event_time,
                eventJson = rawJson
            )
        )

        if (event.object_type == "activity" && (event.aspect_type == "create" || event.aspect_type == "update")) {
            val stravaLink = stravaUserLinksRepo.findByAthleteId(event.owner_id)
                ?: return "OK"

            jobProducer.enqueueActivityIngestJob(stravaLink.userId, event.object_id)
        }

        return "OK"
    }
}
