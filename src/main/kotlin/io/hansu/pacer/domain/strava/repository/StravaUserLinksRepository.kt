package io.hansu.pacer.domain.strava.repository

import io.hansu.pacer.domain.strava.StravaUserLinksEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StravaUserLinksRepository : JpaRepository<StravaUserLinksEntity, Long> {
    fun findByAthleteId(athleteId: Long): StravaUserLinksEntity?
    fun findByUserId(userId: Long): List<StravaUserLinksEntity>
}
