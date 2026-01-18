package io.hansu.pacer.domain.user

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class UserEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    val id: Long = 0,

    @Column(name = "nickname", nullable = false, length = 50)
    val nickname: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "email", unique = true)
    val email: String? = null,

    @Column(name = "provider", length = 20)
    val provider: String? = null,

    @Column(name = "provider_id")
    val providerId: String? = null
)