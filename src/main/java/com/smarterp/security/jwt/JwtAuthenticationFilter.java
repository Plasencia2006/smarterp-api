package com.smarterp.security.jwt;

import com.smarterp.common.exceptions.BusinessException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

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

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                // Extraer TODA la información del token JWT
                Claims claims = jwtTokenProvider.getAllClaimsFromToken(jwt);

                String email = claims.getSubject();
                String userId = claims.get("userId", String.class);
                String businessId = claims.get("businessId", String.class);
                String role = claims.get("role", String.class);

                // Validar que tengamos la información necesaria
                if (userId == null || businessId == null || role == null) {
                    log.error("Token inválido: faltan claims requeridos");
                    response.sendError(401, "Token inválido: información incompleta");
                    return;
                }

                // Crear autenticación con la información del token
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        email, // Principal
                        null, // Credentials (no necesitamos password)
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Guardar información en atributos del request para usar en controladores
                request.setAttribute("userId", userId);
                request.setAttribute("businessId", businessId);
                request.setAttribute("userRole", role);

                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Usuario autenticado: {} | Business: {} | Role: {}",
                        email, businessId, role);
            }
        } catch (BusinessException ex) {
            log.error("Error de autenticación: {}", ex.getMessage());
            response.sendError(401, ex.getMessage());
            return;
        } catch (Exception ex) {
            log.error("No se pudo establecer la autenticación del usuario", ex);
            response.sendError(401, "Token inválido o expirado");
            return;
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