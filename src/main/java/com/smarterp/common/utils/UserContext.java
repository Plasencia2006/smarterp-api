package com.smarterp.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@Slf4j
public class UserContext {

    /**
     * Obtiene el email del usuario autenticado (del token JWT)
     */
    public String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth != null && auth.isAuthenticated() ? auth.getName() : null;
        log.debug("📧 CurrentUserEmail: {}", email);
        return email;
    }

    /**
     * Obtiene el user_id del token JWT (viene de la API de Auth)
     */
    public String getCurrentUserId() {
        HttpServletRequest request = getCurrentRequest();
        String userId = request != null ? (String) request.getAttribute("userId") : null;
        log.debug("👤 CurrentUserId: {}", userId);
        return userId;
    }

    /**
     * Obtiene el business_id del token JWT (viene de la API de Auth)
     */
    public String getCurrentBusinessId() {
        HttpServletRequest request = getCurrentRequest();
        String businessId = request != null ? (String) request.getAttribute("businessId") : null;
        log.debug("🏢 CurrentBusinessId: {}", businessId);
        return businessId;
    }

    /**
     * Obtiene el role del usuario del token JWT
     */
    public String getCurrentUserRole() {
        HttpServletRequest request = getCurrentRequest();
        String role = request != null ? (String) request.getAttribute("userRole") : null;
        log.debug("🔐 CurrentUserRole: {}", role);
        return role;
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}