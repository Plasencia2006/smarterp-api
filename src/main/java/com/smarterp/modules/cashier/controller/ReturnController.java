package com.smarterp.modules.cashier.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.modules.cashier.entity.ReturnOrder;
import com.smarterp.modules.cashier.repository.ReturnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cashier/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnRepository returnRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<ReturnOrder>> processReturn(@RequestBody ReturnOrder returnOrder) {
        return ResponseEntity.ok(ApiResponse.success(returnRepository.save(returnOrder)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<ReturnOrder>>> getMyReturns() {
        return ResponseEntity.ok(ApiResponse.success(returnRepository.findAll()));
    }
}