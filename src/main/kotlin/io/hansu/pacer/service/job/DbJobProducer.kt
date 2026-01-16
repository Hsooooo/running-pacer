package io.hansu.pacer.service.job

import io.hansu.pacer.domain.job.IngestJobEntity
import io.hansu.pacer.domain.job.repository.IngestJobRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class DbJobProducer(
    private val jobRepo: IngestJobRepository
) : JobProducer {

    override fun enqueueActivityIngestJob(userId: Long, activityId: Long) {
        val existing = jobRepo.findAll().find {
            it.provider == IngestJobEntity.PROVIDER_STRAVA &&
            it.providerRefId == activityId &&
            it.jobType == IngestJobEntity.JOB_TYPE_ACTIVITY_UPSERT
        }

        if (existing != null && existing.status != IngestJobEntity.STATUS_FAILED) {
            return
        }

        jobRepo.save(
            IngestJobEntity(
                provider = IngestJobEntity.PROVIDER_STRAVA,
                providerRefId = activityId,
                userId = userId,
                jobType = IngestJobEntity.JOB_TYPE_ACTIVITY_UPSERT,
                status = IngestJobEntity.STATUS_PENDING,
                nextRunAt = LocalDateTime.now()
            )
        )
    }
}
