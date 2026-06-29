package com.smarterp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

        @Override
        public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Todas las rutas
                                .allowedOriginPatterns("*") // ✅ Permitir TODOS los orígenes
                                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS") // Todos los métodos
                                .allowedHeaders("*") // Todos los headers
                                .allowCredentials(true) // Permitir credenciales (cookies, auth)
                                .maxAge(3600); // Cache de preflight: 1 hora
        }

        @Bean
        public CorsFilter corsFilter() {
                CorsConfiguration config = new CorsConfiguration();

                // 🔓 PERMITIR TODO (para desarrollo)
                config.setAllowCredentials(true);
                config.addAllowedOriginPattern("*"); // Permitir todos los orígenes
                config.addAllowedHeader("*"); // Permitir todos los headers
                config.addAllowedMethod("*"); // Permitir todos los métodos
                config.setMaxAge(3600L);

                // Exponer headers adicionales
                config.addExposedHeader("Authorization");
                config.addExposedHeader("X-Business-ID");

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);

                return new CorsFilter(source);
        }
}