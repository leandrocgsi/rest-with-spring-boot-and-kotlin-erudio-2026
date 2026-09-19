package br.com.erudio.config

import br.com.erudio.security.jwt.JwtTokenFilter
import br.com.erudio.security.jwt.JwtTokenProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@EnableWebSecurity
@Configuration
class SecurityConfig {

    @Autowired
    private lateinit var tokenProvider: JwtTokenProvider

    @Bean
    fun passwordEncoder(): PasswordEncoder {

        val pbkdf2Encoder = Pbkdf2PasswordEncoder(
            "", 8, 185000,
            Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256
        )

        val encoders: MutableMap<String, PasswordEncoder> = HashMap()
        encoders["pbkdf2"] = pbkdf2Encoder
        val passwordEncoder = DelegatingPasswordEncoder("pbkdf2", encoders)

        passwordEncoder.setDefaultPasswordEncoderForMatches(pbkdf2Encoder)
        return passwordEncoder
    }

    @Bean
    fun authenticationManager(configuration: AuthenticationConfiguration): AuthenticationManager {
        return configuration.authenticationManager
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val filter = JwtTokenFilter(tokenProvider)
        return http
            .httpBasic { it.disable() }
            .csrf { it.disable() }
            .addFilterBefore(filter, UsernamePasswordAuthenticationFilter::class.java)
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { authorizeHttpRequests ->
                authorizeHttpRequests
                    .requestMatchers(
                        "/auth/signin",
                        "/auth/refresh/**",
                        "/auth/createUser",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                    ).permitAll()
                    .requestMatchers("/api/**").authenticated()
                    .requestMatchers("/users").denyAll()
            }
            .cors { }
            .build()
    }
}
