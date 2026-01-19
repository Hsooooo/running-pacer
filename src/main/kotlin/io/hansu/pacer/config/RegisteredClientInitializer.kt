package io.hansu.pacer.config

import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import java.time.Duration
import java.util.UUID

@Configuration
class RegisteredClientInitializer(
    private val registeredClientRepository: RegisteredClientRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @Bean
    fun initRegisteredClient(): ApplicationRunner {
        return ApplicationRunner {
            val existing = registeredClientRepository.findByClientId("chatgpt-mcp")
            if (existing == null) {
                val client = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("chatgpt-mcp")
                    .clientSecret(passwordEncoder.encode("chatgpt-secret"))
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .redirectUri("https://chatgpt.com/connector_platform_oauth_redirect")
                    .redirectUri("https://platform.openai.com/apps-manage/oauth")
                    .scope("openid")
                    .scope("offline_access")
                    .scope("mcp")
                    .clientSettings(
                        ClientSettings.builder()
                            .requireProofKey(true)
                            .requireAuthorizationConsent(false)
                            .build()
                    )
                    .tokenSettings(
                        TokenSettings.builder()
                            .authorizationCodeTimeToLive(Duration.ofMinutes(5))
                            .accessTokenTimeToLive(Duration.ofHours(1))
                            .refreshTokenTimeToLive(Duration.ofDays(30))
                            .reuseRefreshTokens(false)
                            .build()
                    )
                    .build()

                registeredClientRepository.save(client)
            }
        }
    }
}
