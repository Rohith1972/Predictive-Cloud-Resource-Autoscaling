package com.project.autoscaling.config;

import com.project.autoscaling.security.CustomUserDetailsService;
import com.project.autoscaling.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;
    private final com.project.autoscaling.security.JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final com.project.autoscaling.security.JwtAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health"
                        ).permitAll()

                        // ADMIN only operations
                        .requestMatchers(HttpMethod.POST, "/api/v1/applications/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/applications/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/applications/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/applications/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/v1/instances/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/instances/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/v1/scaling/policies/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/scaling/policies/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/scaling/policies/**").hasRole("ADMIN")

                        .requestMatchers("/api/v1/scaling/recommendations/*/approve").hasRole("ADMIN")
                        .requestMatchers("/api/v1/scaling/recommendations/*/reject").hasRole("ADMIN")
                        .requestMatchers("/api/v1/scaling/evaluate/**").hasRole("ADMIN")

                        .requestMatchers("/api/v1/simulation/start/**", "/api/v1/simulation/stop/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/alerts/**").hasRole("ADMIN")

                        // All other authenticated endpoints (accessible by ADMIN and VIEWER)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
