package com.smarterp.modules.admin.aspect;

import com.smarterp.modules.admin.dto.AuditLogRequest;
import com.smarterp.modules.admin.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogService auditLogService;

    /**
     * ✅ AUDITAR DESPUÉS DE MÉTODOS CREATE
     */
    @AfterReturning(pointcut = "@annotation(com.smarterp.modules.admin.annotation.AuditCreate)", returning = "result", argNames = "joinPoint,result")
    public void auditCreate(JoinPoint joinPoint, Object result) {
        try {
            String entityType = joinPoint.getTarget().getClass().getSimpleName();
            String entityId = extractId(result);

            auditLogService.log("CREATE", entityType, entityId,
                    String.format("Created %s with ID: %s", entityType, entityId));
        } catch (Exception e) {
            log.error("❌ Error auditando CREATE: {}", e.getMessage());
        }
    }

    /**
     * ✅ AUDITAR DESPUÉS DE MÉTODOS UPDATE
     */
    @AfterReturning(pointcut = "@annotation(com.smarterp.modules.admin.annotation.AuditUpdate)", returning = "result", argNames = "joinPoint,result")
    public void auditUpdate(JoinPoint joinPoint, Object result) {
        try {
            String entityType = joinPoint.getTarget().getClass().getSimpleName();
            String entityId = extractId(result);

            auditLogService.log("UPDATE", entityType, entityId,
                    String.format("Updated %s with ID: %s", entityType, entityId));
        } catch (Exception e) {
            log.error("❌ Error auditando UPDATE: {}", e.getMessage());
        }
    }

    /**
     *  AUDITAR DESPUÉS DE MÉTODOS DELETE
     */
    @AfterReturning(pointcut = "@annotation(com.smarterp.modules.admin.annotation.AuditDelete)", argNames = "joinPoint")
    public void auditDelete(JoinPoint joinPoint) {
        try {
            String entityType = joinPoint.getTarget().getClass().getSimpleName();
            Object[] args = joinPoint.getArgs();
            String entityId = args.length > 0 ? String.valueOf(args[0]) : "unknown";

            auditLogService.log("DELETE", entityType, entityId,
                    String.format("Deleted %s with ID: %s", entityType, entityId));
        } catch (Exception e) {
            log.error("❌ Error auditando DELETE: {}", e.getMessage());
        }
    }

    /**
     * ❌ AUDITAR ERRORES
     */
    @AfterThrowing(pointcut = "execution(* com.smarterp.modules..*.*(..))", throwing = "error")
    public void auditException(JoinPoint joinPoint, Throwable error) {
        try {
            auditLogService.log("ERROR",
                    joinPoint.getTarget().getClass().getSimpleName(),
                    joinPoint.getSignature().getName(),
                    String.format("Error: %s", error.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error auditando excepción: {}", e.getMessage());
        }
    }

    // Helper para extraer ID
    private String extractId(Object entity) {
        try {
            if (entity == null)
                return "unknown";

            // Intentar obtener el ID mediante reflexión
            var method = entity.getClass().getMethod("getId");
            Object id = method.invoke(entity);
            return id != null ? id.toString() : "unknown";

        } catch (Exception e) {
            return "unknown";
        }
    }
}