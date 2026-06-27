package com.smarterp.modules.admin.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.admin.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;
    private final UserContext userContext;

    /**
     * 📊 OBTENER ESTADÍSTICAS DEL DASHBOARD
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        String businessId = userContext.getCurrentBusinessId();
        try {
            Map<String, Object> stats = dashboardService.getDashboardStats(businessId);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("❌ Error al obtener estadísticas del dashboard: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 📈 ACTIVIDAD RECIENTE
     */
    @GetMapping("/recent-activity")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecentActivity() {
        String businessId = userContext.getCurrentBusinessId();
        try {
            List<Map<String, Object>> activity = dashboardService.getRecentActivity(businessId);
            return ResponseEntity.ok(ApiResponse.success(activity));
        } catch (Exception e) {
            log.error("❌ Error al obtener actividad reciente: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}