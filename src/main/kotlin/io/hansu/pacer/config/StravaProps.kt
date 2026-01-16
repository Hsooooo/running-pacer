package io.hansu.pacer.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "strava")
data class StravaProps(
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String,
    val webhookVerifyToken: String,
    val baseUrl: String,
    val apiBaseUrl: String
)