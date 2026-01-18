package io.hansu.pacer.service

import io.hansu.pacer.domain.auth.ApiTokenEntity
import io.hansu.pacer.domain.auth.repository.ApiTokenRepository
import io.hansu.pacer.domain.user.UserEntity
import io.hansu.pacer.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.Base64

@Service
@Transactional(readOnly = true)
class ApiTokenService(
    private val apiTokenRepository: ApiTokenRepository,
    private val userRepository: UserRepository
) {
    private val secureRandom = SecureRandom()

    @Transactional
    fun createToken(userId: Long, name: String): ApiTokenEntity {
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("User not found") }
        val token = generateToken()
        
        val apiToken = ApiTokenEntity(
            user = user,
            token = token,
            name = name,
            createdAt = LocalDateTime.now()
        )
        
        return apiTokenRepository.save(apiToken)
    }

    fun listTokens(userId: Long): List<ApiTokenEntity> {
        return apiTokenRepository.findAllByUserId(userId)
    }

    @Transactional
    fun deleteToken(userId: Long, tokenId: Long) {
        val token = apiTokenRepository.findById(tokenId).orElseThrow { IllegalArgumentException("Token not found") }
        if (token.user.id != userId) {
            throw IllegalArgumentException("Access denied")
        }
        apiTokenRepository.delete(token)
    }

    @Transactional
    fun verifyToken(tokenString: String): UserEntity? {
        val token = apiTokenRepository.findByToken(tokenString) ?: return null
        
        token.lastUsedAt = LocalDateTime.now()
        
        return token.user
    }

    private fun generateToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return "sk-running-" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
