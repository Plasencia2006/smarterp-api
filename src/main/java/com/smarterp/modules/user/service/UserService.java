package com.smarterp.modules.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class UserService {

    // ✅ Cache local de nombres
    private final Map<String, String> userNamesCache = new ConcurrentHashMap<>();

    /**
     * 🔍 Obtener nombre del usuario (SIN consultar Django)
     * Solo formatea el userId para que sea legible
     */
    public String getUserFullName(String userId) {
        if (userId == null || userId.isEmpty()) {
            return "N/A";
        }

        // Si ya está en cache, retornar
        if (userNamesCache.containsKey(userId)) {
            return userNamesCache.get(userId);
        }

        String fullName;

        // Si es email temporal tipo "user-123456@temp.com"
        if (userId.contains("@temp.com")) {
            String number = userId.replace("user-", "").replace("@temp.com", "");
            fullName = "Cajero " + number;
        }
        // Si es "temp-user-id" (valor por defecto antiguo)
        else if (userId.equals("temp-user-id")) {
            fullName = "Cajero Desconocido";
        }
        // Si es "user@example.com" (valor por defecto antiguo)
        else if (userId.equals("user@example.com")) {
            fullName = "Usuario Demo";
        }
        // Si es email normal tipo "cajero@techzone.com"
        else if (userId.contains("@")) {
            fullName = userId.split("@")[0];
        }
        // Si es solo un ID
        else {
            fullName = "Usuario " + userId.substring(0, Math.min(8, userId.length()));
        }

        // Guardar en cache
        userNamesCache.put(userId, fullName);
        log.debug("✅ Nombre generado para userId {}: {}", userId, fullName);

        return fullName;
    }
}