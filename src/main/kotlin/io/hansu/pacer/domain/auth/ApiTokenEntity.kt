package io.hansu.pacer.domain.auth

import io.hansu.pacer.domain.user.UserEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "api_tokens")
class ApiTokenEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,

    @Column(name = "token", nullable = false, unique = true, length = 64)
    val token: String,

    @Column(name = "name", nullable = false, length = 100)
    val name: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "expires_at")
    val expiresAt: LocalDateTime? = null,

    @Column(name = "last_used_at")
    var lastUsedAt: LocalDateTime? = null
)
