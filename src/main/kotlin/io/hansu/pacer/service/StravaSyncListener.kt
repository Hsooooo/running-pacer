package io.hansu.pacer.service

import io.hansu.pacer.domain.strava.repository.StravaUserLinksRepository
import io.hansu.pacer.event.StravaLinkCreatedEvent
import io.hansu.pacer.service.job.JobProducer
import io.hansu.pacer.util.StravaApiClient
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class StravaSyncListener(
    private val stravaApiClient: StravaApiClient,
    private val stravaUserLinksRepo: StravaUserLinksRepository,
    private val jobProducer: JobProducer
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Async
    @EventListener
    fun handleStravaLinkCreated(event: StravaLinkCreatedEvent) {
        val userId = event.userId
        val athleteId = event.athleteId

        val link = stravaUserLinksRepo.findByAthleteId(athleteId) ?: return
        if (link.initialSyncDone) return

        logger.info("Starting initial sync for user $userId (last 6 months)")
        
        val after = LocalDateTime.now().minusMonths(6).toEpochSecond(ZoneOffset.UTC)
        
        try {
            var page = 1
            while (true) {
                val activities = stravaApiClient.getAthleteActivities(userId, after = after, page = page, perPage = 30)
                if (activities.isEmpty) break
                
                for (act in activities) {
                    val actId = act["id"].asLong()
                    jobProducer.enqueueActivityIngestJob(userId, actId)
                }
                
                if (activities.size() < 30) break
                page++
            }
            
            link.initialSyncDone = true
            stravaUserLinksRepo.save(link)
            logger.info("Initial sync completed for user $userId")
            
        } catch (e: Exception) {
            logger.error("Failed initial sync for user $userId", e)
        }
    }
}
