package io.hansu.pacer.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(StravaProps::class)
class HttpConfig {
    @Bean
    fun restClient(): RestClient = RestClient.builder().build()
}