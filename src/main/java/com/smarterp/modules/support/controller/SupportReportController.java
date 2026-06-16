package com.smarterp.modules.support.controller;

import com.smarterp.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/support/reports")
public class SupportReportController {

    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTicketReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("totalTickets", 0);
        report.put("openTickets", 0);
        report.put("closedTickets", 0);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/services")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getServiceReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("totalServices", 0);
        report.put("completedServices", 0);
        return ResponseEntity.ok(ApiResponse.success(report));
    }
}