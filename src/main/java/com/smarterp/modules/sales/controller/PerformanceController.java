package com.smarterp.modules.sales.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.sales.entity.SalesCommission;
import com.smarterp.modules.sales.entity.SalesGoal;
import com.smarterp.modules.sales.repository.SalesCommissionRepository;
import com.smarterp.modules.sales.repository.SalesGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class PerformanceController {

    private final SalesCommissionRepository commissionRepository;
    private final SalesGoalRepository goalRepository;
    private final UserContext userContext;

    @GetMapping("/performance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPerformance() {
        String userId = userContext.getCurrentUserId();
        Map<String, Object> performance = new HashMap<>();
        performance.put("commissions", commissionRepository.findBySellerId(userId));
        performance.put("goals", goalRepository.findBySellerId(userId));
        return ResponseEntity.ok(ApiResponse.success(performance));
    }

    @GetMapping("/commissions")
    public ResponseEntity<ApiResponse<List<SalesCommission>>> getMyCommissions() {
        String userId = userContext.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(commissionRepository.findBySellerId(userId)));
    }

    @GetMapping("/goals")
    public ResponseEntity<ApiResponse<List<SalesGoal>>> getMyGoals() {
        String userId = userContext.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(goalRepository.findBySellerId(userId)));
    }
}