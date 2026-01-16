package io.hansu.pacer.domain.strava

import jakarta.persistence.*

@Entity
@Table(
    name = "strava_user_links",
    uniqueConstraints = [UniqueConstraint(name = "uk_strava_user", columnNames = ["user_id", "athlete_id"])],
    indexes = [Index(name = "idx_athlete_id", columnList = "athlete_id")]
)
class StravaUserLinksEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "athlete_id", nullable = false, unique = true)
    val athleteId: Long,

    @Column(name = "scope", length = 255)
    val scope: String? = null,

    @Column(name = "status", nullable = false, length = 20)
    val status: String = "ACTIVE",

    @Column(name = "created_at", nullable = false)
    val createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
)
