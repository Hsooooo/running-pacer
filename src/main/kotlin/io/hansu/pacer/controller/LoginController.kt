package io.hansu.pacer.controller

import io.hansu.pacer.domain.strava.repository.StravaUserLinksRepository
import io.hansu.pacer.service.ApiTokenService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class LoginController(
    private val stravaUserLinksRepository: StravaUserLinksRepository,
    private val apiTokenService: ApiTokenService,
    private val runningQueryService: io.hansu.pacer.service.RunningQueryService
) {

    @GetMapping("/login")
    fun loginPage(): String {
        return "login"
    }

    @GetMapping("/")
    fun home(
        @AuthenticationPrincipal principal: OAuth2User?,
        model: Model
    ): String {
        if (principal == null) {
            return "redirect:/login"
        }

        val userId = principal.attributes["userId"] as Long
        val isStravaLinked = stravaUserLinksRepository.existsByUserId(userId)
        
        // API 토큰 목록 조회 (없으면 빈 리스트)
        val tokens = apiTokenService.listTokens(userId)

        // 최근 30일 활동 조회 (최대 10개)
        val toDate = java.time.LocalDate.now()
        val fromDate = toDate.minusDays(30)
        val recentActivities = runningQueryService.listActivities(userId, fromDate, toDate, 10)

        model.addAttribute("name", principal.attributes["name"])
        model.addAttribute("email", principal.attributes["email"])
        model.addAttribute("isStravaLinked", isStravaLinked)
        model.addAttribute("tokens", tokens)
        model.addAttribute("recentActivities", recentActivities)

        return "home"
    }
}
