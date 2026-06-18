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
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource; // ✅ AGREGAR

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ✅ HABILITAR CORS (ESTA ES LA LÍNEA CLAVE)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // ✅ DESHABILITAR CSRF
                .csrf(csrf -> csrf.disable())

                // ✅ SIN SESIONES
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ✅ AUTORIZACIONES
                .authorizeHttpRequests(auth -> auth
                        // Públicos
                        .requestMatchers("/test/**").permitAll()
                        .requestMatchers("/api/test/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // ✅ PERMITIR PREFLIGHT CORS (OPTIONS)
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

                        // Por rol
                        .requestMatchers("/api/cashier/**").hasAnyRole("CAJERO", "ADMIN")
                        .requestMatchers("/api/sales/**").hasAnyRole("VENDEDOR", "ADMIN")
                        .requestMatchers("/api/inventory/**").hasAnyRole("INVENTARIO", "ADMIN")
                        .requestMatchers("/api/accounting/**").hasAnyRole("CONTADOR", "ADMIN")
                        .requestMatchers("/api/support/**").hasAnyRole("SOPORTE", "ADMIN")

                        // Cualquier otra requiere autenticación
                        .anyRequest().authenticated())

                // ✅ Manejo de excepciones
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"error\": \"Token inválido o ausente\", \"message\": \"Debe proporcionar un token JWT válido\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"error\": \"Acceso denegado\", \"message\": \"No tiene permisos\"}");
                        }))

                // ✅ Filtro JWT
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}