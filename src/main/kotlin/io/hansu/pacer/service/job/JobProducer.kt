package io.hansu.pacer.service.job

interface JobProducer {
    fun enqueueActivityIngestJob(userId: Long, activityId: Long)
}
