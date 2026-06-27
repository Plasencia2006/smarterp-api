package com.smarterp.modules.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarterp.modules.admin.dto.AuditLogDTO;
import com.smarterp.modules.admin.dto.AuditLogRequest;
import com.smarterp.modules.admin.entity.AuditLog;
import com.smarterp.modules.admin.repository.AuditLogRepository;
import com.smarterp.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserContext userContext;
    private final ObjectMapper objectMapper;

    /**
     * 📝 REGISTRAR AUDITORÍA
     */
    @Transactional
    public void logAudit(AuditLogRequest request) {
        try {
            String businessId = userContext.getCurrentBusinessId();
            String userId = userContext.getCurrentUserId();
            String userEmail = userContext.getCurrentUserEmail();

            // ✅ OBTENER NOMBRE DE USUARIO (fallback a email si no existe)
            String userName = getUserNameSafely(userEmail);

            // Obtener IP y User Agent
            HttpServletRequest requestContext = getCurrentRequest();
            String ipAddress = requestContext != null ? requestContext.getRemoteAddr() : "N/A";
            String userAgent = requestContext != null ? requestContext.getHeader("User-Agent") : "N/A";

            AuditLog auditLog = AuditLog.builder()
                    .businessId(businessId)
                    .userId(userId)
                    .userEmail(userEmail)
                    .userName(userName)
                    .action(request.getAction())
                    .entityType(request.getEntityType())
                    .entityId(request.getEntityId())
                    .oldValues(request.getOldValues())
                    .newValues(request.getNewValues())
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .status(request.getStatus() != null ? request.getStatus() : "SUCCESS")
                    .details(request.getDetails())
                    .build();

            auditLogRepository.save(auditLog);

            log.info("📋 Audit: {} - {} - {} by {}",
                    request.getAction(),
                    request.getEntityType(),
                    request.getEntityId(),
                    userEmail);

        } catch (Exception e) {
            log.error("❌ Error al registrar auditoría: {}", e.getMessage(), e);
        }
    }

    /**
     * 📝 MÉTODO RÁPIDO PARA LOGUEAR
     */
    public void log(String action, String entityType, String entityId, String details) {
        AuditLogRequest request = AuditLogRequest.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .status("SUCCESS")
                .build();
        logAudit(request);
    }

    /**
     * 📝 LOG CON VALORES ANTIGUOS Y NUEVOS
     */
    public void logWithValues(String action, String entityType, String entityId,
            Object oldValue, Object newValue, String details) {
        try {
            AuditLogRequest request = AuditLogRequest.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .oldValues(oldValue != null ? objectMapper.writeValueAsString(oldValue) : null)
                    .newValues(newValue != null ? objectMapper.writeValueAsString(newValue) : null)
                    .details(details)
                    .status("SUCCESS")
                    .build();
            logAudit(request);
        } catch (JsonProcessingException e) {
            log.error("❌ Error serializando valores: {}", e.getMessage());
            log(action, entityType, entityId, details);
        }
    }

    /**
     * 📊 OBTENER LOGS CON FILTROS
     */
    @Transactional(readOnly = true)
    public Page<AuditLogDTO> getAuditLogs(AuditLogRequest filters, Pageable pageable) {
        String businessId = userContext.getCurrentBusinessId();
        LocalDateTime start = filters.getStartDate() != null ? filters.getStartDate()
                : LocalDateTime.now().minusDays(30);
        LocalDateTime end = filters.getEndDate() != null ? filters.getEndDate() : LocalDateTime.now();

        Page<AuditLog> logs = auditLogRepository.findAdvanced(
                businessId,
                filters.getAction(),
                filters.getEntityType(),
                filters.getUserId(),
                start,
                end,
                pageable);

        return logs.map(this::toDTO);
    }

    /**
     * 📊 ÚLTIMAS ACTIVIDADES
     */
    @Transactional(readOnly = true)
    public List<AuditLogDTO> getRecentActivities(int limit) {
        String businessId = userContext.getCurrentBusinessId();
        return auditLogRepository.findTop10ByBusinessIdOrderByCreatedAtDesc(businessId)
                .stream()
                .limit(limit)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 📊 ESTADÍSTICAS POR ACCIÓN
     */
    @Transactional(readOnly = true)
    public List<Object[]> getActionStats(LocalDateTime startDate, LocalDateTime endDate) {
        String businessId = userContext.getCurrentBusinessId();
        return auditLogRepository.countByAction(businessId, startDate, endDate);
    }

    /**
     * 🔍 BUSCAR POR ENTIDAD
     */
    @Transactional(readOnly = true)
    public Page<AuditLogDTO> getByEntity(String entityType, String entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable)
                .map(this::toDTO);
    }

    /**
     * 🔍 BUSCAR POR USUARIO
     */
    @Transactional(readOnly = true)
    public Page<AuditLogDTO> getByUser(String userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable)
                .map(this::toDTO);
    }

    // ==================== HELPERS ====================

    /**
     * ✅ OBTENER NOMBRE DE USUARIO DE FORMA SEGURA
     * Intenta obtener el nombre real, si no puede, usa el email
     */
    private String getUserNameSafely(String userEmail) {
        if (userEmail == null || userEmail.isEmpty()) {
            return "Sistema";
        }

        // Intentar extraer nombre del email (parte antes del @)
        if (userEmail.contains("@")) {
            String name = userEmail.split("@")[0];
            // Si es un email temporal tipo user-123456@temp.com
            if (name.startsWith("user-")) {
                return "Cajero " + name.replace("user-", "");
            }
            return name;
        }

        return userEmail;
    }

    private AuditLogDTO toDTO(AuditLog log) {
        return AuditLogDTO.builder()
                .id(log.getId())
                .businessId(log.getBusinessId())
                .userId(log.getUserId())
                .userEmail(log.getUserEmail())
                .userName(log.getUserName())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .oldValues(log.getOldValues())
                .newValues(log.getNewValues())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .status(log.getStatus())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
}