package io.hansu.pacer.mcp

import org.springframework.stereotype.Component

@Component
class McpUserContext {
    fun getCurrentUserId(): Long {
        return 1L
    }
}
