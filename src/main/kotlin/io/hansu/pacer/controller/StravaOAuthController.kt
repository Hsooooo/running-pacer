package io.hansu.pacer.controller

import io.hansu.pacer.config.StravaProps
import io.hansu.pacer.service.StravaAuthService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@RestController
class StravaOAuthController(
    private val stravaAuthService: StravaAuthService,
    private val props: StravaProps
) {
    @GetMapping("/oauth/strava/authorize")
    fun getAuthorizeUrl(): Map<String, String> {
        val redirectUriEncoded = URLEncoder.encode(props.redirectUri, StandardCharsets.UTF_8)
        val url = "https://www.strava.com/oauth/authorize?client_id=${props.clientId}&response_type=code&redirect_uri=$redirectUriEncoded&scope=read,activity:read_all&approval_prompt=auto"
        return mapOf("url" to url)
    }

    @GetMapping("/oauth/strava/callback")
    fun callback(@RequestParam code: String): String {
        stravaAuthService.exchangeCodeAndSaveTokens(code)
        return "ok"
    }
}