package io.hansu.pacer.domain.auth.repository

import io.hansu.pacer.domain.auth.ApiTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ApiTokenRepository : JpaRepository<ApiTokenEntity, Long> {
    fun findByToken(token: String): ApiTokenEntity?
    fun findAllByUserId(userId: Long): List<ApiTokenEntity>
}
