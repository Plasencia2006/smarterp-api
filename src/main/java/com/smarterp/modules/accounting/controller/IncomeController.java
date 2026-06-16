package com.smarterp.modules.accounting.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.accounting.entity.Income;
import com.smarterp.modules.accounting.entity.IncomeCategory;
import com.smarterp.modules.accounting.repository.IncomeRepository;
import com.smarterp.modules.accounting.repository.IncomeCategoryRepository;
import com.smarterp.shared.entity.Business;
import com.smarterp.shared.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounting/incomes")
@RequiredArgsConstructor
public class IncomeController {

    private final IncomeRepository incomeRepository;
    private final IncomeCategoryRepository categoryRepository;
    private final BusinessRepository businessRepository;
    private final UserContext userContext;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Income>>> getIncomes() {
        String businessId = userContext.getCurrentBusinessId();
        return ResponseEntity.ok(ApiResponse.success(incomeRepository.findByBusinessId(businessId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Income>> createIncome(@RequestBody Income income) {
        String businessId = userContext.getCurrentBusinessId();
        Business business = businessRepository.findById(businessId).orElseThrow();
        income.setBusiness(business);
        return ResponseEntity.ok(ApiResponse.success("Ingreso registrado", incomeRepository.save(income)));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<IncomeCategory>>> getCategories() {
        String businessId = userContext.getCurrentBusinessId();
        return ResponseEntity.ok(ApiResponse.success(categoryRepository.findByBusinessId(businessId)));
    }
}