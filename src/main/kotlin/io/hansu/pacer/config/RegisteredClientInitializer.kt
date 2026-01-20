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
            initChatGptClient()
            initClaudeClient()
        }
    }

    private fun initChatGptClient() {
        if (registeredClientRepository.findByClientId("chatgpt-mcp") != null) return

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

    private fun initClaudeClient() {
        if (registeredClientRepository.findByClientId("claude-mcp") != null) return

        val client = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("claude-mcp")
            .clientSecret(passwordEncoder.encode("claude-mcp-secret"))
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            // Claude 웹 콜백 URLs
            .redirectUri("https://claude.ai/api/mcp/auth_callback")
            .redirectUri("https://claude.com/api/mcp/auth_callback")
            // 로컬 개발/테스트용 (Claude Code, MCP Inspector)
            .redirectUri("http://localhost:6274/oauth/callback")
            .redirectUri("http://localhost:6274/oauth/callback/debug")
            .scope("openid")
            .scope("offline_access")
            .scope("mcp")
            .clientSettings(
                ClientSettings.builder()
                    .requireProofKey(true)  // PKCE 필수
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
