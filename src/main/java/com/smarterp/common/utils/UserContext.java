package com.smarterp.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class UserContext {

    /**
     * Obtiene el email del usuario autenticado (del token JWT)
     */
    public String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() ? auth.getName() : null;
    }

    /**
     * Obtiene el user_id del token JWT (viene de la API de Auth)
     */
    public String getCurrentUserId() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? (String) request.getAttribute("userId") : null;
    }

    /**
     * Obtiene el business_id del token JWT (viene de la API de Auth)
     */
    public String getCurrentBusinessId() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? (String) request.getAttribute("businessId") : null;
    }

    /**
     * Obtiene el role del usuario del token JWT
     */
    public String getCurrentUserRole() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? (String) request.getAttribute("userRole") : null;
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}