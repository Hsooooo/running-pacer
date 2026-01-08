package io.hansu.pacer.domain.stat

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "daily_stats")
@IdClass(DailyStatsId::class)
class DailyStatsEntity(
    @Id @Column(name = "user_id")
    val userId: Long,

    @Id @Column(name = "stat_date")
    val statDate: LocalDate,

    @Column(name = "run_count", nullable = false)
    val runCount: Int,

    @Column(name = "total_distance_m", nullable = false)
    val totalDistanceM: Int,

    @Column(name = "total_moving_time_s", nullable = false)
    val totalMovingTimeS: Int,

    @Column(name = "avg_pace_sec_per_km")
    val avgPaceSecPerKm: Int? = null,

    @Column(name = "avg_hr")
    val avgHr: Int? = null
)