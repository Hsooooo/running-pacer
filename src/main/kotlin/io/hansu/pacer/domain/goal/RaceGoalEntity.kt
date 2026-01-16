package io.hansu.pacer.domain.goal

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "race_goals")
class RaceGoalEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "goal_id")
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "race_name", nullable = false)
    val raceName: String,

    @Column(name = "race_date", nullable = false)
    val raceDate: LocalDate,

    @Column(name = "race_distance_m", nullable = false)
    val raceDistanceM: Int,

    @Column(name = "target_pace_sec_per_km", nullable = false)
    val targetPaceSecPerKm: Int,

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: GoalStatus = GoalStatus.ACTIVE,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class GoalStatus {
    ACTIVE, COMPLETED, CANCELLED
}
