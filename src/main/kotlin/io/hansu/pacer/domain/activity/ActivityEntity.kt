package io.hansu.pacer.domain.activity

import jakarta.persistence.*
import lombok.NoArgsConstructor
import java.time.LocalDateTime

@Entity
@Table(
    name = "activities",
    uniqueConstraints = [UniqueConstraint(name = "uk_activity_source", columnNames = ["source", "source_activity_id"])],
    indexes = [
        Index(name = "idx_activity_user_time", columnList = "user_id,start_time_utc"),
        Index(name = "idx_activity_user_dist", columnList = "user_id,distance_m")
    ]
)
@NoArgsConstructor
class ActivityEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_id")
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "source", nullable = false, length = 20)
    val source: String = "STRAVA",

    @Column(name = "source_activity_id", nullable = false)
    val sourceActivityId: Long,

    @Column(name = "sport_type", nullable = false, length = 20)
    val sportType: String = "RUN",

    @Column(name = "start_time_utc", nullable = false)
    val startTimeUtc: LocalDateTime,

    @Column(name = "timezone", length = 50)
    val timezone: String? = null,

    @Column(name = "distance_m", nullable = false)
    val distanceM: Int,

    @Column(name = "moving_time_s", nullable = false)
    val movingTimeS: Int,

    @Column(name = "elapsed_time_s", nullable = false)
    val elapsedTimeS: Int,

    @Column(name = "avg_hr")
    val avgHr: Int? = null,

    @Column(name = "max_hr")
    val maxHr: Int? = null,

    @Column(name = "avg_cadence")
    val avgCadence: Int? = null,

    @Column(name = "elevation_gain_m")
    val elevationGainM: Int? = null,

    @Column(name = "avg_pace_sec_per_km")
    val avgPaceSecPerKm: Int? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)