package com.smarterp.config;

import com.smarterp.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ✅ PÚBLICOS - Sin autenticación
                        .requestMatchers("/test/**").permitAll()
                        .requestMatchers("/api/test/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // 🔒 PROTEGIDOS - Requieren token JWT válido y rol específico
                        .requestMatchers("/api/cashier/**").hasAnyRole("CAJERO", "ADMIN")
                        .requestMatchers("/api/sales/**").hasAnyRole("VENDEDOR", "ADMIN")
                        .requestMatchers("/api/inventory/**").hasAnyRole("INVENTARIO", "ADMIN")
                        .requestMatchers("/api/accounting/**").hasAnyRole("CONTADOR", "ADMIN")
                        .requestMatchers("/api/support/**").hasAnyRole("SOPORTE", "ADMIN")

                        // Todo lo demás requiere autenticación
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"error\": \"Token inválido o ausente\", \"message\": \"Debe proporcionar un token JWT válido en el header Authorization\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"error\": \"Acceso denegado\", \"message\": \"No tiene permisos para acceder a este recurso\"}");
                        }))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}