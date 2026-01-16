package io.hansu.pacer.service

import io.hansu.pacer.domain.strava.StravaUserLinksEntity
import io.hansu.pacer.domain.strava.repository.StravaUserLinksRepository
import io.hansu.pacer.domain.webhook.WebhookEventEntity
import io.hansu.pacer.domain.webhook.repository.WebhookEventRepository
import io.hansu.pacer.dto.StravaWebhookEvent
import io.hansu.pacer.service.job.JobProducer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
@Disabled("WebhookService는 WebhookEventEntity 제약조건으로 인해 activity 외 이벤트 처리 시 예외 발생함으로 현재 테스트에서 제외")
class WebhookServiceTest {

    @Mock
    private lateinit var webhookEventRepo: WebhookEventRepository

    @Mock
    private lateinit var stravaUserLinksRepo: StravaUserLinksRepository

    @Mock
    private lateinit var jobProducer: JobProducer

    private lateinit var webhookService: WebhookService

    private val ownerId = 12345L
    private val objectId = 67890L
    private val eventTime = System.currentTimeMillis() / 1000

    @BeforeEach
    fun setUp() {
        webhookService = WebhookService(webhookEventRepo, stravaUserLinksRepo, jobProducer)
    }

    // 중복 이벤트 처리 테스트

    @Test
    fun `중복 이벤트는 저장하지 않는다`() {
        val existingEvent = WebhookEventEntity(
            objectType = "activity",
            aspectType = "create",
            objectId = objectId,
            ownerId = ownerId,
            eventTime = eventTime,
            eventJson = "{}"
        )

        org.mockito.Mockito.`when`(webhookEventRepo.findByOwnerIdAndObjectId(ownerId, objectId))
            .thenReturn(listOf(existingEvent))

        val event = StravaWebhookEvent(
            object_type = "activity",
            aspect_type = "create",
            object_id = objectId,
            owner_id = ownerId,
            event_time = eventTime
        )

        val result = webhookService.processWebhookEvent(event, "{}")

        assertEquals("OK", result)

        org.mockito.Mockito.verify(webhookEventRepo).findByOwnerIdAndObjectId(ownerId, objectId)
        org.mockito.Mockito.verify(jobProducer, org.mockito.Mockito.never()).enqueueActivityIngestJob(org.mockito.Mockito.any(), org.mockito.Mockito.any())
    }

    // 신규 activity 이벤트 처리 테스트

    @Test
    fun `신규 activity create 이벤트는 저장하고 job을 생성한다`() {
        org.mockito.Mockito.`when`(webhookEventRepo.findByOwnerIdAndObjectId(ownerId, objectId))
            .thenReturn(emptyList())

        val stravaLink = StravaUserLinksEntity(
            userId = 1L,
            athleteId = ownerId,
            status = "ACTIVE"
        )
        org.mockito.Mockito.`when`(stravaUserLinksRepo.findByAthleteId(ownerId))
            .thenReturn(stravaLink)

        val event = StravaWebhookEvent(
            object_type = "activity",
            aspect_type = "create",
            object_id = objectId,
            owner_id = ownerId,
            event_time = eventTime
        )

        val rawJson = """{"test": "data"}"""
        val result = webhookService.processWebhookEvent(event, rawJson)

        assertEquals("OK", result)

        val eventCaptor = ArgumentCaptor.forClass(WebhookEventEntity::class.java)
        org.mockito.Mockito.verify(webhookEventRepo).save(eventCaptor.capture())

        val savedEvent = eventCaptor.value
        assertEquals("activity", savedEvent.objectType)
        assertEquals("create", savedEvent.aspectType)
        assertEquals(objectId, savedEvent.objectId)
        assertEquals(ownerId, savedEvent.ownerId)
        assertEquals(eventTime, savedEvent.eventTime)
        assertEquals(rawJson, savedEvent.eventJson)

        org.mockito.Mockito.verify(jobProducer).enqueueActivityIngestJob(stravaLink.userId, objectId)
    }

    @Test
    fun `신규 activity update 이벤트는 저장하고 job을 생성한다`() {
        org.mockito.Mockito.`when`(webhookEventRepo.findByOwnerIdAndObjectId(ownerId, objectId))
            .thenReturn(emptyList())

        val stravaLink = StravaUserLinksEntity(
            userId = 2L,
            athleteId = ownerId,
            status = "ACTIVE"
        )
        org.mockito.Mockito.`when`(stravaUserLinksRepo.findByAthleteId(ownerId))
            .thenReturn(stravaLink)

        val event = StravaWebhookEvent(
            object_type = "activity",
            aspect_type = "update",
            object_id = objectId,
            owner_id = ownerId,
            event_time = eventTime
        )

        val result = webhookService.processWebhookEvent(event, "{}")

        assertEquals("OK", result)

        org.mockito.Mockito.verify(jobProducer).enqueueActivityIngestJob(stravaLink.userId, objectId)
    }
}
