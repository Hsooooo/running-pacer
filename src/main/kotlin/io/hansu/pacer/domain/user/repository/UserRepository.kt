package io.hansu.pacer.domain.user.repository

import io.hansu.pacer.domain.user.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<UserEntity, Long> {
    fun findByEmail(email: String): UserEntity?
    fun findByProviderAndProviderId(provider: String, providerId: String): UserEntity?
    fun findFirstByOrderByCreatedAtAsc(): UserEntity?
}
