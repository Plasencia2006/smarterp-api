package com.smarterp.modules.cashier.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.modules.cashier.entity.Payment;
import com.smarterp.modules.cashier.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/cashier/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRepository paymentRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<Payment>> processPayment(@RequestBody Payment payment) {
        return ResponseEntity.ok(ApiResponse.success(paymentRepository.save(payment)));
    }

    @GetMapping("/methods")
    public ResponseEntity<ApiResponse<List<String>>> getPaymentMethods() {
        return ResponseEntity.ok(ApiResponse.success(Arrays.asList("CASH", "CARD", "TRANSFER", "MIXED")));
    }
}