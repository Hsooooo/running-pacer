package io.hansu.pacer.domain.activity

import jakarta.persistence.*

@Entity
@Table(
    name = "laps",
    uniqueConstraints = [UniqueConstraint(name = "uk_lap", columnNames = ["activity_id", "lap_index"])],
    indexes = [Index(name = "idx_lap_activity", columnList = "activity_id")]
)
class LapEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lap_id")
    val id: Long = 0,

    @Column(name = "activity_id", nullable = false)
    val activityId: Long,

    @Column(name = "lap_index", nullable = false)
    val lapIndex: Int,

    @Column(name = "distance_m", nullable = false)
    val distanceM: Int,

    @Column(name = "moving_time_s", nullable = false)
    val movingTimeS: Int,

    @Column(name = "avg_hr")
    val avgHr: Int? = null,

    @Column(name = "avg_cadence")
    val avgCadence: Int? = null,

    @Column(name = "elevation_gain_m")
    val elevationGainM: Int? = null,

    @Column(name = "avg_pace_sec_per_km")
    val avgPaceSecPerKm: Int? = null
)
