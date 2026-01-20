package io.hansu.pacer.config

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import java.security.KeyFactory

import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

@Configuration
class AuthorizationServerConfig(
    @Value("\${oauth.issuer}") private val issuer: String,
    @Value("\${oauth.rsa.public-key}") private val publicKeyPem: String,
    @Value("\${oauth.rsa.private-key}") private val privateKeyPem: String
) {

    @Bean
    @Order(1)
    fun authorizationServerSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val authorizationServerConfigurer = OAuth2AuthorizationServerConfigurer()
        http.with(authorizationServerConfigurer) {
            it.oidc(Customizer.withDefaults())
        }

        http.securityMatcher(authorizationServerConfigurer.endpointsMatcher)
            .authorizeHttpRequests { authorize ->
                authorize.anyRequest().authenticated()
            }
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint(LoginUrlAuthenticationEntryPoint("/login"))
            }
            .oauth2Login(Customizer.withDefaults())

        return http.build()
    }

    @Bean
    fun registeredClientRepository(jdbcTemplate: JdbcTemplate): RegisteredClientRepository {
        return JdbcRegisteredClientRepository(jdbcTemplate)
    }

    @Bean
    fun authorizationService(
        jdbcTemplate: JdbcTemplate,
        registeredClientRepository: RegisteredClientRepository
    ): OAuth2AuthorizationService {
        return JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository)
    }

    @Bean
    fun authorizationConsentService(
        jdbcTemplate: JdbcTemplate,
        registeredClientRepository: RegisteredClientRepository
    ): OAuth2AuthorizationConsentService {
        return JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository)
    }

    @Bean
    fun jwkSource(): JWKSource<SecurityContext> {
        if (publicKeyPem.isBlank() || privateKeyPem.isBlank()) {
            throw IllegalStateException("OAuth RSA keys are missing")
        }
        val publicKey = parsePublicKey(publicKeyPem)
        val privateKey = parsePrivateKey(privateKeyPem)
        val rsaKey = RSAKey.Builder(publicKey).privateKey(privateKey).keyID("running-pacer-rsa").build()
        return ImmutableJWKSet(JWKSet(rsaKey))
    }

    @Bean
    fun authorizationServerSettings(): AuthorizationServerSettings {
        return AuthorizationServerSettings.builder()
            .issuer(issuer)
            .build()
    }

    @Bean
    fun jwtCustomizer(): OAuth2TokenCustomizer<JwtEncodingContext> {
        return OAuth2TokenCustomizer { context ->
            if (OAuth2TokenType.ACCESS_TOKEN == context.tokenType) {
                val userId = extractUserId(context.getPrincipal())
                if (userId != null) {
                    context.claims.claim("user_id", userId.toString())
                }
            }
        }
    }

    private fun extractUserId(authentication: Authentication): Long? {
        val principal = authentication.principal
        if (principal is OAuth2User) {
            val raw = principal.attributes["userId"] ?: return null
            return when (raw) {
                is Number -> raw.toLong()
                is String -> raw.toLongOrNull()
                else -> null
            }
        }
        return null
    }

    private fun parsePublicKey(pem: String): RSAPublicKey {
        val content = stripPem(pem)
        val bytes = Base64.getDecoder().decode(content)
        val keySpec = X509EncodedKeySpec(bytes)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePublic(keySpec) as RSAPublicKey
    }

    private fun parsePrivateKey(pem: String): RSAPrivateKey {
        val content = stripPem(pem)
        val bytes = Base64.getDecoder().decode(content)
        val keySpec = PKCS8EncodedKeySpec(bytes)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePrivate(keySpec) as RSAPrivateKey
    }

    private fun stripPem(pem: String): String {
        return pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .trim()
    }
}
