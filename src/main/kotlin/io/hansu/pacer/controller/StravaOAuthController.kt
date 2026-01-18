package io.hansu.pacer.controller

import io.hansu.pacer.config.StravaProps
import io.hansu.pacer.service.StravaAuthService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64

@RestController
class StravaOAuthController(
    private val stravaAuthService: StravaAuthService,
    private val props: StravaProps
) {
    @GetMapping("/oauth/strava/authorize")
    fun getAuthorizeUrl(@AuthenticationPrincipal principal: OAuth2User?): Map<String, String> {
        val userId = principal?.attributes?.get("userId") as? Long 
            ?: throw IllegalStateException("User must be logged in")

        val state = Base64.getUrlEncoder().encodeToString(userId.toString().toByteArray())
        
        val redirectUriEncoded = URLEncoder.encode(props.redirectUri, StandardCharsets.UTF_8)
        val url = "https://www.strava.com/oauth/authorize?client_id=${props.clientId}&response_type=code&redirect_uri=$redirectUriEncoded&scope=read,activity:read_all&approval_prompt=auto&state=$state"
        return mapOf("url" to url)
    }

    @GetMapping("/oauth/strava/callback")
    fun callback(
        @RequestParam code: String,
        @RequestParam state: String
    ): String {
        val userIdStr = String(Base64.getUrlDecoder().decode(state))
        val userId = userIdStr.toLongOrNull() ?: throw IllegalArgumentException("Invalid state")

        stravaAuthService.exchangeCodeAndSaveTokens(code, userId)
        return "ok"
    }
}
