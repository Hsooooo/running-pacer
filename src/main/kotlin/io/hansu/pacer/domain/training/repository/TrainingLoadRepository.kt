package io.hansu.pacer.domain.training.repository

import io.hansu.pacer.domain.training.TrainingLoadEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface TrainingLoadRepository : JpaRepository<TrainingLoadEntity, Long> {
    fun findByActivityId(activityId: Long): TrainingLoadEntity?
    fun findByUserIdAndTrainingDateBetween(userId: Long, from: LocalDate, to: LocalDate): List<TrainingLoadEntity>

    @Query("SELECT AVG(t.tss) FROM TrainingLoadEntity t WHERE t.userId = :userId AND t.trainingDate >= :fromDate")
    fun getAverageTssSince(userId: Long, fromDate: LocalDate): Double?
}
