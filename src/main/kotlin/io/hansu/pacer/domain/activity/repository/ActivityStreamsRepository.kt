package io.hansu.pacer.domain.activity.repository

import io.hansu.pacer.domain.activity.ActivityStreamsEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface ActivityStreamsJpaRepository : JpaRepository<ActivityStreamsEntity, Long>

@Repository
class ActivityStreamsRepository(
    private val jpa: ActivityStreamsJpaRepository
) {
    fun findByIdOrNull(activityId: Long): ActivityStreamsEntity? =
        jpa.findById(activityId).orElse(null)

    fun upsert(entity: ActivityStreamsEntity) {
        jpa.save(entity)
    }
}