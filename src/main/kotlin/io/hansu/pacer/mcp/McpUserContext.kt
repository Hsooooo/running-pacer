package io.hansu.pacer.mcp

import io.hansu.pacer.domain.user.repository.UserRepository
import org.springframework.stereotype.Component

@Component
class McpUserContext(
    private val userRepository: UserRepository
) {
    fun getCurrentUserId(): Long {
        val user = userRepository.findFirstByOrderByCreatedAtAsc()
            ?: throw IllegalStateException("No users found in database")
        return user.id
    }
}
