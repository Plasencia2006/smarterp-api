package com.smarterp.modules.admin.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.modules.admin.dto.AuditLogDTO;
import com.smarterp.modules.admin.dto.AuditLogRequest;
import com.smarterp.modules.admin.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
@Slf4j
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * 📊 OBTENER LOGS DE AUDITORÍA
     * GET
     * /admin/audit/logs?page=0&size=20&action=USER_CREATE&startDate=2026-01-01&endDate=2026-06-26
     */
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        try {
            AuditLogRequest filters = AuditLogRequest.builder()
                    .action(action)
                    .entityType(entityType)
                    .userId(userId)
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();

            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<AuditLogDTO> logs = auditLogService.getAuditLogs(filters, pageRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("content", logs.getContent());
            response.put("totalElements", logs.getTotalElements());
            response.put("totalPages", logs.getTotalPages());
            response.put("currentPage", logs.getNumber());

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("❌ Error al obtener logs: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 📊 ÚLTIMAS ACTIVIDADES
     * GET /admin/audit/recent?limit=10
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<AuditLogDTO>>> getRecentActivities(
            @RequestParam(defaultValue = "10") int limit) {

        try {
            List<AuditLogDTO> activities = auditLogService.getRecentActivities(limit);
            return ResponseEntity.ok(ApiResponse.success(activities));
        } catch (Exception e) {
            log.error("❌ Error al obtener actividades recientes: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 📊 ESTADÍSTICAS POR ACCIÓN
     * GET /admin/audit/stats?startDate=2026-01-01&endDate=2026-06-26
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActionStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        try {
            LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusDays(30);
            LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

            List<Object[]> stats = auditLogService.getActionStats(start, end);

            Map<String, Long> actionCounts = new HashMap<>();
            for (Object[] stat : stats) {
                actionCounts.put((String) stat[0], (Long) stat[1]);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("stats", actionCounts);
            response.put("startDate", start);
            response.put("endDate", end);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("❌ Error al obtener estadísticas: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 🔍 BUSCAR POR ENTIDAD
     * GET /admin/audit/entity/User/123abc?page=0&size=10
     */
    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getByEntity(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<AuditLogDTO> logs = auditLogService.getByEntity(entityType, entityId, pageRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("content", logs.getContent());
            response.put("totalElements", logs.getTotalElements());
            response.put("totalPages", logs.getTotalPages());

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("❌ Error al buscar por entidad: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 🔍 BUSCAR POR USUARIO
     * GET /admin/audit/user/userId123?page=0&size=10
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<AuditLogDTO> logs = auditLogService.getByUser(userId, pageRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("content", logs.getContent());
            response.put("totalElements", logs.getTotalElements());
            response.put("totalPages", logs.getTotalPages());

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error(" Error al buscar por usuario: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}