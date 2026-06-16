package com.smarterp.modules.accounting.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.accounting.entity.Expense;
import com.smarterp.modules.accounting.entity.ExpenseCategory;
import com.smarterp.modules.accounting.repository.ExpenseRepository;
import com.smarterp.modules.accounting.repository.ExpenseCategoryRepository;
import com.smarterp.shared.entity.Business;
import com.smarterp.shared.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounting/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final BusinessRepository businessRepository;
    private final UserContext userContext;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Expense>>> getExpenses() {
        String businessId = userContext.getCurrentBusinessId();
        return ResponseEntity.ok(ApiResponse.success(expenseRepository.findByBusinessId(businessId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Expense>> createExpense(@RequestBody Expense expense) {
        String businessId = userContext.getCurrentBusinessId();
        Business business = businessRepository.findById(businessId).orElseThrow();
        expense.setBusiness(business);
        return ResponseEntity.ok(ApiResponse.success("Gasto registrado", expenseRepository.save(expense)));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<ExpenseCategory>>> getCategories() {
        String businessId = userContext.getCurrentBusinessId();
        return ResponseEntity.ok(ApiResponse.success(categoryRepository.findByBusinessId(businessId)));
    }
}