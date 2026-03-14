package com.gamifyapi.config;

import com.gamifyapi.repository.ApiKeyRepository;
import com.gamifyapi.repository.TenantRepository;
import com.gamifyapi.security.ApiKeyAuthenticationFilter;
import com.gamifyapi.security.CustomUserDetailsService;
import com.gamifyapi.security.JwtAuthenticationFilter;
import com.gamifyapi.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuração central do Spring Security.
 * Define regras de acesso, filtros de autenticação e política de sessão STATELESS.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final TenantRepository tenantRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> {})
            .authorizeHttpRequests(auth -> auth
                // Rotas públicas
                .requestMatchers(
                    "/api/v1/auth/register",
                    "/api/v1/auth/login",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/h2-console/**"
                ).permitAll()
                // Rotas de integração via API Key
                .requestMatchers(
                    "/api/v1/actions",
                    "/api/v1/players/**",
                    "/api/v1/leaderboard/**"
                ).authenticated()
                // Demais rotas exigem JWT (admin)
                .anyRequest().authenticated()
            )
            // H2 console usa frames
            .headers(h -> h.frameOptions(fo -> fo.sameOrigin()))
            // Filtro JWT antes do padrão do Spring
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtTokenProvider, tenantRepository),
                UsernamePasswordAuthenticationFilter.class
            )
            // Filtro de API Key antes do JWT
            .addFilterBefore(
                new ApiKeyAuthenticationFilter(apiKeyRepository),
                JwtAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
