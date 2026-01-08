package io.hansu.pacer.domain.activity.repository

import io.hansu.pacer.domain.activity.LapEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

interface LapJpaRepository : JpaRepository<LapEntity, Long> {
    @Modifying
    @Query("delete from LapEntity l where l.activityId = :activityId")
    fun deleteByActivityId(activityId: Long): Int

    fun findAllByActivityIdOrderByLapIndexAsc(activityId: Long): List<LapEntity>
}

@Repository
class LapRepository(
    private val jpa: LapJpaRepository
) {
    @Transactional
    fun upsertAll(activityId: Long, laps: List<LapEntity>) {
        // 기존 랩 삭제 후 재삽입 (Bulk Insert가 아니어도 saveAll은 배치 처리됨)
        jpa.deleteByActivityId(activityId)
        jpa.saveAll(laps)
    }

    fun findAll(activityId: Long): List<LapEntity> =
        jpa.findAllByActivityIdOrderByLapIndexAsc(activityId)
}