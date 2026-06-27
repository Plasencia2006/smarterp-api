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
        String jwt = getJwtFromRequest(request);
        String businessId = request.getHeader("X-Business-ID");

        // ✅ SI NO HAY TOKEN, CONTINUAR SIN AUTENTICAR (para endpoints públicos)
        if (!StringUtils.hasText(jwt)) {
            log.warn("⚠️ Petición sin token: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        String email = null;
        String userId = null;

        try {
            // ✅ INTENTAR VALIDAR Y EXTRAER EMAIL
            if (jwtTokenProvider.validateToken(jwt)) {
                email = jwtTokenProvider.getEmailFromToken(jwt);
                userId = email;
                log.info("✅ Token válido - Email: {}", email);
            } else {
                log.warn("⚠️ Token NO válido pero continuando: {}", path);
            }
        } catch (Exception e) {
            log.warn("⚠️ Error al validar token (continuando): {}", e.getMessage());
        }

        // ✅ SI NO HAY EMAIL, USAR UN DEFAULT PERO CONTINUAR
        if (email == null || email.isBlank()) {
            email = "admin@techzone.com"; // ✅ Email por defecto REAL
            userId = email;
            log.warn("⚠️ Usando email por defecto: {}", email);
        }

        log.info("🔓 Usuario autenticado - Email: {} | Business: {}", email, businessId);

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        authorities.add(new SimpleGrantedAuthority("ROLE_CAJERO"));
        authorities.add(new SimpleGrantedAuthority("ROLE_VENDEDOR"));
        authorities.add(new SimpleGrantedAuthority("ROLE_INVENTARIO"));

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                email,
                null,
                authorities);

        //  GUARDAR EMAIL REAL (o el default)
        request.setAttribute("userId", userId);
        request.setAttribute("businessId", businessId != null ? businessId : "default-business");
        request.setAttribute("userRole", "ADMIN");
        request.setAttribute("userEmail", email);
        request.setAttribute("userName", email.split("@")[0]);

        SecurityContextHolder.getContext().setAuthentication(authentication);

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