package io.hansu.pacer.service.job

import io.hansu.pacer.domain.job.IngestJobEntity
import io.hansu.pacer.domain.job.repository.IngestJobRepository
import io.hansu.pacer.util.StravaApiClient
import io.hansu.pacer.service.StravaIngestService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class IngestJobScheduler(
    private val jobRepo: IngestJobRepository,
    private val stravaApiClient: StravaApiClient,
    private val stravaIngestService: StravaIngestService
) {
    private val logger = LoggerFactory.getLogger(IngestJobScheduler::class.java)

    @Scheduled(fixedDelay = 30000)
    @Transactional
    fun processPendingJobs() {
        val jobs = jobRepo.claimPendingJobs(java.time.LocalDateTime.now(), limit = 10)

        for (job in jobs) {
            processJob(job)
        }
    }

    private fun processJob(job: IngestJobEntity) {
        try {
            jobRepo.markAsRunning(job.id)

            if (job.provider == IngestJobEntity.PROVIDER_STRAVA &&
                job.jobType == IngestJobEntity.JOB_TYPE_ACTIVITY_UPSERT) {
                processStravaActivityJob(job)
            } else {
                logger.warn("Unsupported job type: ${job.provider}/${job.jobType}")
            }

            jobRepo.markAsDone(job.id)
            logger.info("Job completed: {}", job.id)
        } catch (e: Exception) {
            handleJobFailure(job, e)
        }
    }

    private fun processStravaActivityJob(job: IngestJobEntity) {
        val userId = job.userId ?: error("User ID is required for job processing")

        val activityJson = stravaApiClient.getActivity(job.providerRefId)
        val lapsJson = stravaApiClient.getActivityLaps(job.providerRefId)
        val streamsJson = stravaApiClient.getActivityStreams(
            job.providerRefId,
            listOf("time", "distance", "latlng", "altitude", "velocity_smooth", "heartrate", "cadence")
        )

        stravaIngestService.ingest(userId, activityJson, lapsJson, streamsJson)
        logger.info("Ingested Strava activity {}: job {}", job.providerRefId, job.id)
    }

    private fun handleJobFailure(job: IngestJobEntity, error: Exception) {
        logger.error("Job failed: {}", job.id, error)

        if (job.retryCount >= IngestJobEntity.MAX_RETRY_COUNT) {
            jobRepo.markAsFailedPermanently(job.id, error.message ?: "Unknown error")
        } else {
            val backoffSeconds = calculateBackoff(job.retryCount)
            val nextRunAt = java.time.LocalDateTime.now().plusSeconds(backoffSeconds)
            jobRepo.markAsFailedWithRetry(job.id, error.message ?: "Unknown error", nextRunAt)
            logger.info("Job scheduled for retry (attempt {}): next run at {}", job.retryCount + 1, nextRunAt)
        }
    }

    private fun calculateBackoff(retryCount: Int): Long {
        return when (retryCount) {
            0 -> 5 * 60L
            1 -> 15 * 60L
            2 -> 30 * 60L
            3 -> 60 * 60L
            else -> 3 * 60 * 60L
        }
    }
}
