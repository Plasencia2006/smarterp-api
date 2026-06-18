package com.smarterp.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // ⚠️ MODO DESARROLLO: Aceptar cualquier endpoint sin validación estricta
        // TODO: En producción, validar correctamente el JWT

        try {
            String jwt = getJwtFromRequest(request);

            // Obtener businessId del header (enviado por el frontend)
            String businessId = request.getHeader("X-Business-ID");

            if (StringUtils.hasText(jwt)) {
                // Intentar validar el token
                boolean tokenValid = false;
                String email = "user@example.com";
                String userId = "temp-user-id";
                String role = "ADMIN"; // Rol por defecto

                try {
                    if (jwtTokenProvider.validateToken(jwt)) {
                        tokenValid = true;
                        // Intentar extraer claims (puede fallar si los nombres no coinciden)
                        try {
                            email = jwtTokenProvider.getEmailFromToken(jwt);
                        } catch (Exception e) {
                            log.debug("No se pudo extraer email del token");
                        }

                        try {
                            userId = jwtTokenProvider.getUserIdFromToken(jwt);
                        } catch (Exception e) {
                            log.debug("No se pudo extraer userId del token");
                        }
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Token no válido, usando modo permisivo: {}", e.getMessage());
                    // ⚠️ MODO DESARROLLO: Continuar sin rechazar
                    tokenValid = true; // Aceptar de todas formas
                }

                // ⚠️ MODO DESARROLLO: Dar TODOS los roles para poder probar
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                authorities.add(new SimpleGrantedAuthority("ROLE_CAJERO"));
                authorities.add(new SimpleGrantedAuthority("ROLE_VENDEDOR"));
                authorities.add(new SimpleGrantedAuthority("ROLE_INVENTARIO"));
                authorities.add(new SimpleGrantedAuthority("ROLE_CONTADOR"));
                authorities.add(new SimpleGrantedAuthority("ROLE_SOPORTE"));

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        authorities);

                // Guardar información en atributos del request
                request.setAttribute("userId", userId);
                request.setAttribute("businessId", businessId != null ? businessId : "default-business");
                request.setAttribute("userRole", "ADMIN");
                request.setAttribute("userEmail", email);

                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.info("🔓 Usuario autenticado (modo desarrollo): {} | Business: {}",
                        email, businessId);
            } else {
                // ⚠️ MODO DESARROLLO: Permitir peticiones sin token también
                log.warn("⚠️ Petición sin token - permitida en modo desarrollo: {}", path);

                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        "anonymous",
                        null,
                        authorities);

                request.setAttribute("userId", "anonymous");
                request.setAttribute("businessId", businessId != null ? businessId : "default-business");
                request.setAttribute("userRole", "ADMIN");

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (Exception ex) {
            log.error("❌ Error en filtro JWT: {}", ex.getMessage());
            // ⚠️ MODO DESARROLLO: No rechazar, continuar
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}