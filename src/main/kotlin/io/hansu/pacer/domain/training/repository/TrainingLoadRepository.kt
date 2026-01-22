package io.hansu.pacer.domain.training.repository

import io.hansu.pacer.domain.training.TrainingLoadEntity
import java.time.LocalDate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TrainingLoadRepository : JpaRepository<TrainingLoadEntity, Long> {
    fun findByActivityId(activityId: Long): TrainingLoadEntity?
    fun findByUserIdAndTrainingDateBetween(
            userId: Long,
            from: LocalDate,
            to: LocalDate
    ): List<TrainingLoadEntity>

    @Query(
            "SELECT AVG(t.tss) FROM TrainingLoadEntity t WHERE t.userId = :userId AND t.trainingDate >= :fromDate"
    )
    fun getAverageTssSince(userId: Long, fromDate: LocalDate): Double?

    /** 기간 내 워크아웃 타입별 런 횟수 */
    @Query(
            """
        SELECT t.workoutType, COUNT(t) 
        FROM TrainingLoadEntity t 
        WHERE t.userId = :userId 
          AND t.trainingDate >= :fromDate 
          AND t.trainingDate <= :toDate
          AND t.workoutType IS NOT NULL
        GROUP BY t.workoutType
    """
    )
    fun getWorkoutTypeDistribution(
            userId: Long,
            fromDate: LocalDate,
            toDate: LocalDate
    ): List<Array<Any>>

    /** 기간 내 총 TSS */
    @Query(
            "SELECT COALESCE(SUM(t.tss), 0) FROM TrainingLoadEntity t WHERE t.userId = :userId AND t.trainingDate >= :fromDate AND t.trainingDate <= :toDate"
    )
    fun getTotalTss(userId: Long, fromDate: LocalDate, toDate: LocalDate): Double

    /** 최근 N일간 일별 TSS (ATL/CTL 계산용) */
    @Query(
            """
        SELECT t.trainingDate, COALESCE(SUM(t.tss), 0)
        FROM TrainingLoadEntity t 
        WHERE t.userId = :userId 
          AND t.trainingDate >= :fromDate
        GROUP BY t.trainingDate
        ORDER BY t.trainingDate DESC
    """
    )
    fun getDailyTssSince(userId: Long, fromDate: LocalDate): List<Array<Any>>

    /** 기간 내 가장 높은 VDOT 추정치 */
    @Query(
            "SELECT MAX(t.vdotEstimate) FROM TrainingLoadEntity t WHERE t.userId = :userId AND t.trainingDate >= :fromDate AND t.vdotEstimate IS NOT NULL"
    )
    fun getMaxVdotSince(userId: Long, fromDate: LocalDate): Double?
}
