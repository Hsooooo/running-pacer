package io.hansu.pacer.config

import io.hansu.pacer.service.auth.CustomOAuth2UserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val apiTokenFilter: ApiTokenFilter,
    private val mcpAuthenticationEntryPoint: McpAuthenticationEntryPoint
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    @Order(2)
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() } // REST API 위주이므로 CSRF 비활성화
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/", "/error", "/favicon.ico").permitAll()
                    .requestMatchers("/oauth/**", "/login/**").permitAll()
                    .requestMatchers("/webhook/**").permitAll()
                    // OAuth 메타데이터 엔드포인트는 공개
                    .requestMatchers("/.well-known/**").permitAll()
                    // MCP 엔드포인트는 인증 필수 (JWT 또는 API 토큰)
                    .requestMatchers("/sse", "/mcp/**").authenticated()
                    .anyRequest().permitAll() // 개발 단계이므로 일단 열어둠
            }
            .addFilterBefore(apiTokenFilter, UsernamePasswordAuthenticationFilter::class.java)
            .oauth2Login { oauth2 ->
                oauth2
                    .loginPage("/login")
                    .defaultSuccessUrl("/", true)
                    .authorizationEndpoint {
                        it.baseUri("/oauth2/authorization")
                    }
                    .redirectionEndpoint {
                        it.baseUri("/login/oauth2/code/*")
                    }
                    .userInfoEndpoint {
                        it.userService(customOAuth2UserService)
                    }
            }
            .oauth2ResourceServer { resourceServer ->
                resourceServer
                    .jwt {}
                    .authenticationEntryPoint(mcpAuthenticationEntryPoint)
            }

        return http.build()
    }
}
