package com.knowledge.api.config;

import com.knowledge.api.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer; // Import Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults()) // Use Customizer.withDefaults() for CORS
                .csrf(csrf -> csrf.disable()) // Disable CSRF
                .formLogin(login -> login.disable()) // Disable form login
                .httpBasic(basic -> basic.disable()) // Disable HTTP Basic authentication
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/").permitAll() // Allow access to the root path
                        .requestMatchers("/api/v1/auth/**").permitAll() // Allow access to auth endpoints
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll() // Allow access to Swagger UI
                        .requestMatchers("/api/search/searchData").permitAll() // Allow access to Swagger UI
                        .requestMatchers("/api/v1/knowledge/feed/**").authenticated() // Require authentication for
                        .requestMatchers("/api/v1/dashboard/**").authenticated()

                        .anyRequest().authenticated() // Require authentication for all other endpoints
                )
                .sessionManagement(management -> management
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Use stateless sessions
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class); // Add JWT filter

        return http.build();
    }
}