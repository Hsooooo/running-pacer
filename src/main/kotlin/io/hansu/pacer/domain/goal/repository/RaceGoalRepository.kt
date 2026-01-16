package io.hansu.pacer.domain.goal.repository

import io.hansu.pacer.domain.goal.GoalStatus
import io.hansu.pacer.domain.goal.RaceGoalEntity
import org.springframework.data.jpa.repository.JpaRepository

interface RaceGoalRepository : JpaRepository<RaceGoalEntity, Long> {
    fun findByUserIdAndStatus(userId: Long, status: GoalStatus): List<RaceGoalEntity>
    fun findFirstByUserIdAndStatusOrderByRaceDateAsc(userId: Long, status: GoalStatus): RaceGoalEntity?
}
