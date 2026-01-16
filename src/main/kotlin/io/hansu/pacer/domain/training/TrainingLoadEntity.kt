package io.hansu.pacer.domain.training

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "training_loads")
class TrainingLoadEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "load_id")
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "activity_id", nullable = false)
    val activityId: Long,

    @Column(name = "training_date", nullable = false)
    val trainingDate: LocalDate,

    @Column(name = "tss")
    val tss: BigDecimal? = null,

    @Column(name = "intensity_factor")
    val intensityFactor: BigDecimal? = null,

    @Column(name = "workout_type")
    @Enumerated(EnumType.STRING)
    val workoutType: WorkoutType? = null,

    @Column(name = "vdot_estimate")
    val vdotEstimate: BigDecimal? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class WorkoutType {
    EASY, TEMPO, INTERVAL, LONG_RUN, RECOVERY
}
