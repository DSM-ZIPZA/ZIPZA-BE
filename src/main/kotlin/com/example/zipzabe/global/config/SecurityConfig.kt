package com.example.zipzabe.global.config

import com.example.zipzabe.global.auth.jwt.JwtAuthenticationFilter
import com.example.zipzabe.global.auth.oauth2.KakaoOAuth2UserService
import com.example.zipzabe.global.auth.oauth2.OAuth2FailureHandler
import com.example.zipzabe.global.auth.oauth2.OAuth2SuccessHandler
import com.example.zipzabe.global.error.GlobalExceptionFilter
import com.example.zipzabe.global.error.exception.ErrorCode
import com.example.zipzabe.global.error.exception.ErrorResponse
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
) {
    @Bean
    fun filterChain(
        http: HttpSecurity,
        kakaoOAuth2UserService: KakaoOAuth2UserService,
        oauth2SuccessHandler: OAuth2SuccessHandler,
        oauth2FailureHandler: OAuth2FailureHandler,
        objectMapper: ObjectMapper,
    ): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .formLogin { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .oauth2Login { oauth2 ->
                oauth2
                    .redirectionEndpoint { it.baseUri("/zipza/oauth2") }
                    .userInfoEndpoint { it.userService(kakaoOAuth2UserService) }
                    .successHandler(oauth2SuccessHandler)
                    .failureHandler(oauth2FailureHandler)
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/actuator/health",
                        "/actuator/health/**",
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling { e ->
                e.authenticationEntryPoint { _, response, _ ->
                    writeUnauthorized(response, objectMapper)
                }
                e.accessDeniedHandler { _, response, _ ->
                    writeUnauthorized(response, objectMapper)
                }
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(GlobalExceptionFilter(objectMapper), JwtAuthenticationFilter::class.java)

        return http.build()
    }

    private fun writeUnauthorized(response: HttpServletResponse, objectMapper: ObjectMapper) {
        response.status = ErrorCode.UNAUTHORIZED.httpStatus.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        objectMapper.writeValue(
            response.writer,
            ErrorResponse(ErrorCode.UNAUTHORIZED.httpStatus, ErrorCode.UNAUTHORIZED.message),
        )
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowedOriginPatterns = listOf("*")
        config.allowedMethods = listOf("*")
        config.allowedHeaders = listOf("*")
        config.exposedHeaders = listOf("*")
        config.allowCredentials = true
        config.maxAge = 3600

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }
}
