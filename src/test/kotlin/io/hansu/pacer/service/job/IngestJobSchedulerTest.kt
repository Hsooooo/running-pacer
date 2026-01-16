package io.hansu.pacer.service.job

import com.fasterxml.jackson.databind.JsonNode
import io.hansu.pacer.domain.job.IngestJobEntity
import io.hansu.pacer.domain.job.repository.IngestJobRepository
import io.hansu.pacer.service.StravaIngestService
import io.hansu.pacer.util.StravaApiClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class IngestJobSchedulerTest {

    @Mock
    private lateinit var jobRepo: IngestJobRepository

    @Mock
    private lateinit var stravaApiClient: StravaApiClient

    @Mock
    private lateinit var stravaIngestService: StravaIngestService

    private lateinit var scheduler: IngestJobScheduler

    private val userId = 1L
    private val activityId = 67890L

    @BeforeEach
    fun setUp() {
        scheduler = IngestJobScheduler(jobRepo, stravaApiClient, stravaIngestService)
    }

    // 정상 job 처리 테스트

    @Test
    fun `PENDING 상태의 job을 처리한다`() {
        val job = IngestJobEntity(
            id = 100L,
            provider = IngestJobEntity.PROVIDER_STRAVA,
            providerRefId = activityId,
            userId = userId,
            jobType = IngestJobEntity.JOB_TYPE_ACTIVITY_UPSERT,
            status = IngestJobEntity.STATUS_PENDING,
            nextRunAt = java.time.LocalDateTime.now()
        )

        org.mockito.Mockito.`when`(jobRepo.claimPendingJobs(org.mockito.Mockito.any(), org.mockito.Mockito.eq(10)))
            .thenReturn(listOf(job))
        org.mockito.Mockito.`when`(stravaApiClient.getActivity(activityId))
            .thenReturn(createMockJsonNode())
        org.mockito.Mockito.`when`(stravaApiClient.getActivityLaps(activityId))
            .thenReturn(createMockJsonNode())
        org.mockito.Mockito.`when`(stravaApiClient.getActivityStreams(org.mockito.Mockito.eq(activityId), org.mockito.Mockito.any()))
            .thenReturn(createMockJsonNode())

        scheduler.processPendingJobs()

        org.mockito.Mockito.verify(jobRepo).claimPendingJobs(org.mockito.Mockito.any(), org.mockito.Mockito.eq(10))
        org.mockito.Mockito.verify(jobRepo).markAsRunning(job.id)
        org.mockito.Mockito.verify(stravaApiClient).getActivity(activityId)
        org.mockito.Mockito.verify(stravaApiClient).getActivityLaps(activityId)
        org.mockito.Mockito.verify(stravaApiClient).getActivityStreams(org.mockito.Mockito.eq(activityId), org.mockito.Mockito.any())
        org.mockito.Mockito.verify(stravaIngestService).ingest(org.mockito.Mockito.eq(userId), org.mockito.Mockito.any(), org.mockito.Mockito.any(), org.mockito.Mockito.any())
        org.mockito.Mockito.verify(jobRepo).markAsDone(job.id)
    }

    // 예외 처리 테스트

    @Test
    fun `job 처리 중 예외가 발생하면 markAsDone는 호출되지 않는다`() {
        val job = IngestJobEntity(
            id = 100L,
            provider = IngestJobEntity.PROVIDER_STRAVA,
            providerRefId = activityId,
            userId = userId,
            jobType = IngestJobEntity.JOB_TYPE_ACTIVITY_UPSERT,
            status = IngestJobEntity.STATUS_PENDING,
            nextRunAt = java.time.LocalDateTime.now()
        )

        org.mockito.Mockito.`when`(jobRepo.claimPendingJobs(org.mockito.Mockito.any(), org.mockito.Mockito.eq(10)))
            .thenReturn(listOf(job))
        org.mockito.Mockito.`when`(stravaApiClient.getActivity(activityId))
            .thenThrow(RuntimeException("API 호출 실패"))

        scheduler.processPendingJobs()

        org.mockito.Mockito.verify(jobRepo).markAsRunning(job.id)
        org.mockito.Mockito.verify(jobRepo, org.mockito.Mockito.never()).markAsDone(org.mockito.Mockito.any())
    }

    // 재시도 정책 테스트

    @Test
    fun `실패한 job에 대해 재시도를 예약한다`() {
        val job = IngestJobEntity(
            id = 100L,
            provider = IngestJobEntity.PROVIDER_STRAVA,
            providerRefId = activityId,
            userId = userId,
            jobType = IngestJobEntity.JOB_TYPE_ACTIVITY_UPSERT,
            status = IngestJobEntity.STATUS_PENDING,
            retryCount = 0,
            nextRunAt = java.time.LocalDateTime.now()
        )

        org.mockito.Mockito.`when`(jobRepo.claimPendingJobs(org.mockito.Mockito.any(), org.mockito.Mockito.eq(10)))
            .thenReturn(listOf(job))
        org.mockito.Mockito.`when`(stravaApiClient.getActivity(activityId))
            .thenThrow(RuntimeException("API 호출 실패"))

        scheduler.processPendingJobs()

        org.mockito.Mockito.verify(jobRepo).markAsRunning(job.id)

        val nextRunCaptor = ArgumentCaptor.forClass(java.time.LocalDateTime::class.java)
        org.mockito.Mockito.verify(jobRepo).markAsFailedWithRetry(org.mockito.Mockito.eq(job.id), org.mockito.Mockito.eq("API 호출 실패"), nextRunCaptor.capture())

        val nextRunAt = nextRunCaptor.value
        val expectedNextRun = java.time.LocalDateTime.now().plusSeconds(5 * 60L)
        val toleranceSeconds = 5
        val diffSeconds = kotlin.math.abs(java.time.Duration.between(expectedNextRun, nextRunAt).seconds)
        assert(diffSeconds <= toleranceSeconds) { "백오프 시간이 예상과 다릅니다: $diffSeconds 초 차이" }
    }

    @Test
    fun `최대 재시도 횟수를 초과한 job은 영구 실패로 표시한다`() {
        val job = IngestJobEntity(
            id = 100L,
            provider = IngestJobEntity.PROVIDER_STRAVA,
            providerRefId = activityId,
            userId = userId,
            jobType = IngestJobEntity.JOB_TYPE_ACTIVITY_UPSERT,
            status = IngestJobEntity.STATUS_PENDING,
            retryCount = IngestJobEntity.MAX_RETRY_COUNT,
            nextRunAt = java.time.LocalDateTime.now()
        )

        org.mockito.Mockito.`when`(jobRepo.claimPendingJobs(org.mockito.Mockito.any(), org.mockito.Mockito.eq(10)))
            .thenReturn(listOf(job))
        org.mockito.Mockito.`when`(stravaApiClient.getActivity(activityId))
            .thenThrow(RuntimeException("지속적인 실패"))

        scheduler.processPendingJobs()

        org.mockito.Mockito.verify(jobRepo).markAsRunning(job.id)
        org.mockito.Mockito.verify(jobRepo).markAsFailedPermanently(org.mockito.Mockito.eq(job.id), org.mockito.Mockito.eq("지속적인 실패"))
    }

    // 지원하지 않는 job 테스트

    @Test
    fun `지원하지 않는 provider의 job은 처리하지 않는다`() {
        val job = IngestJobEntity(
            id = 100L,
            provider = "OTHER_PROVIDER",
            providerRefId = activityId,
            userId = userId,
            jobType = IngestJobEntity.JOB_TYPE_ACTIVITY_UPSERT,
            status = IngestJobEntity.STATUS_PENDING,
            nextRunAt = java.time.LocalDateTime.now()
        )

        org.mockito.Mockito.`when`(jobRepo.claimPendingJobs(org.mockito.Mockito.any(), org.mockito.Mockito.eq(10)))
            .thenReturn(listOf(job))

        scheduler.processPendingJobs()

        org.mockito.Mockito.verify(jobRepo).markAsRunning(job.id)
        org.mockito.Mockito.verify(stravaApiClient, org.mockito.Mockito.never()).getActivity(org.mockito.Mockito.any())
        org.mockito.Mockito.verify(stravaApiClient, org.mockito.Mockito.never()).getActivityLaps(org.mockito.Mockito.any())
        org.mockito.Mockito.verify(stravaApiClient, org.mockito.Mockito.never()).getActivityStreams(org.mockito.Mockito.any(), org.mockito.Mockito.any())
        org.mockito.Mockito.verify(jobRepo).markAsDone(job.id)
    }

    @Test
    fun `지원하지 않는 jobType의 job은 처리하지 않는다`() {
        val job = IngestJobEntity(
            id = 100L,
            provider = IngestJobEntity.PROVIDER_STRAVA,
            providerRefId = activityId,
            userId = userId,
            jobType = "OTHER_TYPE",
            status = IngestJobEntity.STATUS_PENDING,
            nextRunAt = java.time.LocalDateTime.now()
        )

        org.mockito.Mockito.`when`(jobRepo.claimPendingJobs(org.mockito.Mockito.any(), org.mockito.Mockito.eq(10)))
            .thenReturn(listOf(job))

        scheduler.processPendingJobs()

        org.mockito.Mockito.verify(jobRepo).markAsRunning(job.id)
        org.mockito.Mockito.verify(stravaApiClient, org.mockito.Mockito.never()).getActivity(org.mockito.Mockito.any())
        org.mockito.Mockito.verify(jobRepo).markAsDone(job.id)
    }

    private fun createMockJsonNode(): JsonNode {
        val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
        val node = objectMapper.createObjectNode()
        node.put("id", activityId)
        node.put("name", "Test Activity")
        return node
    }
}
