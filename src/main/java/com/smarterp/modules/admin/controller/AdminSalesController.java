package com.smarterp.modules.admin.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.admin.service.AdminSalesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/admin/sales")
@RequiredArgsConstructor
@Slf4j
public class AdminSalesController {

        private final AdminSalesService adminSalesService;
        private final UserContext userContext;

        /**
         * 📊 DASHBOARD DE VENTAS - Vista del Administrador
         * GET /admin/sales/dashboard
         */
        @GetMapping("/dashboard")
        public ResponseEntity<ApiResponse<Map<String, Object>>> getSalesDashboard(
                        @RequestParam(required = false) String startDate,
                        @RequestParam(required = false) String endDate) {

                String businessId = userContext.getCurrentBusinessId();

                LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate + "T00:00:00")
                                : LocalDateTime.now().toLocalDate().atStartOfDay();

                LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate + "T23:59:59") : LocalDateTime.now();

                log.info("📊 Admin obteniendo dashboard de ventas - Business: {} - Desde: {} - Hasta: {}",
                                businessId, start, end);

                try {
                        Map<String, Object> dashboard = adminSalesService.getSalesDashboard(businessId, start, end);
                        return ResponseEntity.ok(ApiResponse.success(dashboard));
                } catch (Exception e) {
                        log.error("❌ Error al obtener dashboard de ventas: {}", e.getMessage());
                        return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
                }
        }

        /**
         * 📋 TODAS LAS VENTAS/COTIZACIONES DEL NEGOCIO
         * GET /admin/sales/quotes
         */
        @GetMapping("/quotes")
        public ResponseEntity<ApiResponse<Map<String, Object>>> getAllQuotes(
                        @RequestParam(required = false, defaultValue = "0") int page,
                        @RequestParam(required = false, defaultValue = "20") int size,
                        @RequestParam(required = false) String status,
                        @RequestParam(required = false) String sellerId,
                        @RequestParam(required = false) String startDate,
                        @RequestParam(required = false) String endDate) {

                String businessId = userContext.getCurrentBusinessId();

                log.info("📋 Admin obteniendo cotizaciones - Business: {} - Page: {} - Size: {}",
                                businessId, page, size);

                try {
                        Map<String, Object> result = adminSalesService.getAllQuotes(
                                        businessId, page, size, status, sellerId, startDate, endDate);
                        return ResponseEntity.ok(ApiResponse.success(result));
                } catch (Exception e) {
                        log.error("❌ Error al obtener cotizaciones: {}", e.getMessage());
                        return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
                }
        }

        /**
         * 💰 VENTAS POR PERÍODO (día, semana, mes)
         * GET /admin/sales/sales-by-period
         */
        @GetMapping("/sales-by-period")
        public ResponseEntity<ApiResponse<Map<String, Object>>> getSalesByPeriod(
                        @RequestParam(required = false, defaultValue = "day") String period,
                        @RequestParam(required = false) String startDate,
                        @RequestParam(required = false) String endDate) {

                String businessId = userContext.getCurrentBusinessId();

                LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate + "T00:00:00")
                                : LocalDateTime.now().minusDays(30).toLocalDate().atStartOfDay();

                LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate + "T23:59:59") : LocalDateTime.now();

                log.info("💰 Admin obteniendo ventas por período: {} - Business: {}", period, businessId);

                try {
                        Map<String, Object> salesByPeriod = adminSalesService.getSalesByPeriod(
                                        businessId, period, start, end);
                        return ResponseEntity.ok(ApiResponse.success(salesByPeriod));
                } catch (Exception e) {
                        log.error("❌ Error al obtener ventas por período: {}", e.getMessage());
                        return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
                }
        }
}