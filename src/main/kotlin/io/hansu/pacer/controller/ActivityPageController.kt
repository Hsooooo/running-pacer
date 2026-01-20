package io.hansu.pacer.controller

import io.hansu.pacer.dto.ActivitySummary
import io.hansu.pacer.service.RunningQueryService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

@Controller
@RequestMapping("/activities")
class ActivityPageController(
    private val queryService: RunningQueryService
) {
    @GetMapping
    fun activityPage(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) from: String?,
        @RequestParam(required = false) to: String?,
        @AuthenticationPrincipal principal: OAuth2User?,
        model: Model
    ): String {
        val userId = extractUserId(principal) ?: return "redirect:/login"
        
        val fromDate = from?.let { LocalDate.parse(it) } ?: LocalDate.now().minusMonths(3)
        val toDate = to?.let { LocalDate.parse(it) } ?: LocalDate.now()
        
        val activities = queryService.listActivities(userId, fromDate, toDate, limit = size * page)
        
        val startIndex = (page - 1) * size
        val pagedActivities = if (startIndex < activities.size) {
            activities.subList(startIndex, minOf(startIndex + size, activities.size))
        } else {
            emptyList()
        }

        model.addAttribute("activities", pagedActivities)
        model.addAttribute("currentPage", page)
        model.addAttribute("fromDate", fromDate)
        model.addAttribute("toDate", toDate)
        
        return "activities"
    }

    private fun extractUserId(principal: OAuth2User?): Long? {
        val raw = principal?.attributes?.get("userId") ?: return null
        return when (raw) {
            is Number -> raw.toLong()
            is String -> raw.toLongOrNull()
            else -> null
        }
    }
}
