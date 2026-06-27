package com.smarterp.modules.admin.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.admin.dto.ReportRequest;
import com.smarterp.modules.admin.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;
    private final UserContext userContext;

    /**
     * 📊 GENERAR REPORTE (Descarga directa)
     * GET
     * /admin/reports/generate?type=SALES&format=CSV&startDate=2026-01-01&endDate=2026-06-26
     */
    @GetMapping("/generate")
    public ResponseEntity<ByteArrayResource> generateReport(
            @RequestParam String type,
            @RequestParam(defaultValue = "CSV") String format,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        String businessId = userContext.getCurrentBusinessId();
        log.info("📊 Generando reporte - Tipo: {} - Formato: {} - Business: {}", type, format, businessId);

        try {
            // Parsear fechas
            LocalDateTime start = parseStartDate(startDate);
            LocalDateTime end = parseEndDate(endDate);

            ReportRequest request = ReportRequest.builder()
                    .reportType(type)
                    .format(format)
                    .startDate(start)
                    .endDate(end)
                    .businessId(businessId)
                    .build();

            ByteArrayResource resource = reportService.generateReport(request);

            // Determinar tipo de contenido y nombre de archivo
            String contentType;
            String fileExtension;
            String fileName;

            if ("EXCEL".equalsIgnoreCase(format)) {
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                fileExtension = "xlsx";
                fileName = type.toLowerCase() + "_reporte_"
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            } else {
                contentType = "text/csv; charset=UTF-8";
                fileExtension = "csv";
                fileName = type.toLowerCase() + "_reporte_"
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(resource.contentLength())
                    .body(resource);

        } catch (Exception e) {
            log.error("❌ Error generando reporte: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 📋 LISTAR TIPOS DE REPORTES DISPONIBLES
     * GET /admin/reports/types
     */
    @GetMapping("/types")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReportTypes() {
        Map<String, Object> reportTypes = new HashMap<>();

        Map<String, String> types = new HashMap<>();
        types.put("SALES", "Reporte de Ventas");
        types.put("INVENTORY", "Reporte de Inventario");
        types.put("CASH", "Reporte de Caja");
        types.put("PRODUCTS", "Reporte de Productos");
        types.put("CUSTOMERS", "Reporte de Clientes");

        reportTypes.put("types", types);
        reportTypes.put("formats", new String[] { "CSV", "EXCEL" });

        return ResponseEntity.ok(ApiResponse.success(reportTypes));
    }

    /**
     * 📊 OBTENER RESUMEN DE DATOS PARA PREVIEW
     * GET /admin/reports/preview?type=SALES&startDate=...&endDate=...
     */
    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReportPreview(
            @RequestParam String type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        String businessId = userContext.getCurrentBusinessId();
        log.info("📊 Preview de reporte - Tipo: {} - Business: {}", type, businessId);

        try {
            LocalDateTime start = parseStartDate(startDate);
            LocalDateTime end = parseEndDate(endDate);

            Map<String, Object> preview = new HashMap<>();
            preview.put("type", type);
            preview.put("startDate", start);
            preview.put("endDate", end);
            preview.put("businessId", businessId);

            // Aquí puedes agregar datos de preview según el tipo
            switch (type.toUpperCase()) {
                case "SALES":
                    preview.put("message", "Se generará reporte de ventas con cotizaciones del período");
                    break;
                case "INVENTORY":
                    preview.put("message", "Se generará reporte completo de inventario");
                    break;
                case "CASH":
                    preview.put("message", "Se generará reporte de movimientos de caja");
                    break;
                case "PRODUCTS":
                    preview.put("message", "Se generará reporte de todos los productos");
                    break;
                case "CUSTOMERS":
                    preview.put("message", "Se generará reporte de clientes con estadísticas");
                    break;
            }

            return ResponseEntity.ok(ApiResponse.success(preview));

        } catch (Exception e) {
            log.error("❌ Error obteniendo preview: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== HELPERS ====================

    private LocalDateTime parseStartDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return LocalDateTime.now().minusDays(30); // Últimos 30 días por defecto
        }
        try {
            return LocalDateTime.parse(dateStr + "T00:00:00");
        } catch (Exception e) {
            return LocalDateTime.now().minusDays(30);
        }
    }

    private LocalDateTime parseEndDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(dateStr + "T23:59:59");
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}