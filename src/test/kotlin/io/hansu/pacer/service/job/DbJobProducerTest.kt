package io.hansu.pacer.service.job

import io.hansu.pacer.domain.job.IngestJobEntity
import io.hansu.pacer.domain.job.repository.IngestJobRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockitoExtension::class)
class DbJobProducerTest {

    @Mock
    private lateinit var jobRepo: IngestJobRepository

    private lateinit var dbJobProducer: DbJobProducer

    private val userId = 1L
    private val activityId = 67890L

    @BeforeEach
    fun setUp() {
        dbJobProducer = DbJobProducer(jobRepo)
    }

    // 중복 job 저장 방지 테스트

    @Test
    fun `기존 PENDING 상태의 동일 job이 있으면 새로 저장하지 않는다`() {
        val existingJob = IngestJobEntity(
            id = 100L,
            provider = IngestJobEntity.PROVIDER_STRAVA,
            providerRefId = activityId,
            userId = userId,
            jobType = IngestJobEntity.JOB_TYPE_ACTIVITY_UPSERT,
            status = IngestJobEntity.STATUS_PENDING,
            nextRunAt = java.time.LocalDateTime.now()
        )

        whenever(jobRepo.findAll())
            .thenReturn(listOf(existingJob))

        dbJobProducer.enqueueActivityIngestJob(userId, activityId)

        verify(jobRepo).findAll()
        verify(jobRepo, never()).save(any())
    }

    @Test
    fun `기존 RUNNING 상태의 동일 job이 있으면 새로 저장하지 않는다`() {
        val existingJob = IngestJobEntity(
            id = 100L,
            provider = IngestJobEntity.PROVIDER_STRAVA,
            providerRefId = activityId,
            userId = userId,
            jobType = IngestJobEntity.JOB_TYPE_ACTIVITY_UPSERT,
            status = IngestJobEntity.STATUS_RUNNING,
            nextRunAt = java.time.LocalDateTime.now()
        )

        whenever(jobRepo.findAll())
            .thenReturn(listOf(existingJob))

        dbJobProducer.enqueueActivityIngestJob(userId, activityId)

        verify(jobRepo, never()).save(any())
    }

    @Test
    fun `기존 DONE 상태의 동일 job이 있으면 새로 저장하지 않는다`() {
        val existingJob = IngestJobEntity(
            id = 100L,
            provider = IngestJobEntity.PROVIDER_STRAVA,
            providerRefId = activityId,
            userId = userId,
            jobType = IngestJobEntity.JOB_TYPE_ACTIVITY_UPSERT,
            status = IngestJobEntity.STATUS_DONE,
            nextRunAt = java.time.LocalDateTime.now()
        )

        whenever(jobRepo.findAll())
            .thenReturn(listOf(existingJob))

        dbJobProducer.enqueueActivityIngestJob(userId, activityId)

        verify(jobRepo, never()).save(any())
    }

    @Test
    fun `기존 FAILED 상태의 동일 job이 있으면 재생성한다`() {
        val existingJob = IngestJobEntity(
            id = 100L,
            provider = IngestJobEntity.PROVIDER_STRAVA,
            providerRefId = activityId,
            userId = userId,
            jobType = IngestJobEntity.JOB_TYPE_ACTIVITY_UPSERT,
            status = IngestJobEntity.STATUS_FAILED,
            nextRunAt = java.time.LocalDateTime.now()
        )

        whenever(jobRepo.findAll())
            .thenReturn(listOf(existingJob))

        dbJobProducer.enqueueActivityIngestJob(userId, activityId)

        verify(jobRepo).save(any())
    }

    @Test
    fun `동일한 job이 없으면 새로 저장한다`() {
        whenever(jobRepo.findAll())
            .thenReturn(emptyList())

        dbJobProducer.enqueueActivityIngestJob(userId, activityId)

        verify(jobRepo).save(any())
    }

    // job 저장 값 검증 테스트

    @Test
    fun `새로 저장된 job에 올바른 값이 설정된다`() {
        whenever(jobRepo.findAll())
            .thenReturn(emptyList())

        dbJobProducer.enqueueActivityIngestJob(userId, activityId)

        val jobCaptor = argumentCaptor<IngestJobEntity>()
        verify(jobRepo).save(jobCaptor.capture())

        val savedJob = jobCaptor.firstValue
        assertNotNull(savedJob)
        assertEquals(IngestJobEntity.PROVIDER_STRAVA, savedJob.provider)
        assertEquals(activityId, savedJob.providerRefId)
        assertEquals(userId, savedJob.userId)
        assertEquals(IngestJobEntity.JOB_TYPE_ACTIVITY_UPSERT, savedJob.jobType)
        assertEquals(IngestJobEntity.STATUS_PENDING, savedJob.status)
        assertEquals(0, savedJob.retryCount)
    }

    // 다른 provider/jobType 처리 테스트

    @Test
    fun `서로 다른 provider의 job은 별도로 저장된다`() {
        whenever(jobRepo.findAll())
            .thenReturn(emptyList())

        val otherActivityId = 99999L
        dbJobProducer.enqueueActivityIngestJob(userId, otherActivityId)

        verify(jobRepo).save(any())
    }

    @Test
    fun `서로 다른 jobType의 job은 별도로 저장된다`() {
        val otherTypeJob = IngestJobEntity(
            id = 100L,
            provider = IngestJobEntity.PROVIDER_STRAVA,
            providerRefId = activityId,
            userId = userId,
            jobType = "OTHER_TYPE",
            status = IngestJobEntity.STATUS_PENDING,
            nextRunAt = java.time.LocalDateTime.now()
        )

        whenever(jobRepo.findAll())
            .thenReturn(listOf(otherTypeJob))

        dbJobProducer.enqueueActivityIngestJob(userId, activityId)

        verify(jobRepo).save(any())
    }
}
