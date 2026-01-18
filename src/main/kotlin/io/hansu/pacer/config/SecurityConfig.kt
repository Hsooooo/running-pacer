package io.hansu.pacer.config

import io.hansu.pacer.service.auth.CustomOAuth2UserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val apiTokenFilter: ApiTokenFilter
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() } // REST API 위주이므로 CSRF 비활성화
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/", "/error", "/favicon.ico").permitAll()
                    .requestMatchers("/oauth/**", "/login/**").permitAll()
                    .requestMatchers("/webhook/**").permitAll()
                    .requestMatchers("/sse", "/mcp/**").authenticated() // API Token 필요
                    .anyRequest().permitAll() // 개발 단계이므로 일단 열어둠
            }
            .addFilterBefore(apiTokenFilter, UsernamePasswordAuthenticationFilter::class.java)
            .oauth2Login { oauth2 ->
                oauth2
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

        return http.build()
    }
}
