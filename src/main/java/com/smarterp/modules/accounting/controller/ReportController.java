package com.smarterp.modules.accounting.controller;

import com.smarterp.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/accounting/reports")
public class ReportController {

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBalanceSheet() {
        Map<String, Object> report = new HashMap<>();
        report.put("assets", 0);
        report.put("liabilities", 0);
        report.put("equity", 0);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/income-statement")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getIncomeStatement() {
        Map<String, Object> report = new HashMap<>();
        report.put("revenue", 0);
        report.put("expenses", 0);
        report.put("netIncome", 0);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/cash-flow")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCashFlow() {
        Map<String, Object> report = new HashMap<>();
        report.put("operatingActivities", 0);
        report.put("investingActivities", 0);
        report.put("financingActivities", 0);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/tax")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTaxReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("taxableIncome", 0);
        report.put("taxDue", 0);
        return ResponseEntity.ok(ApiResponse.success(report));
    }
}