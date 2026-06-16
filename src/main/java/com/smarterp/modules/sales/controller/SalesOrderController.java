package com.smarterp.modules.sales.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.modules.cashier.entity.Sale;
import com.smarterp.modules.cashier.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sales/sales")
@RequiredArgsConstructor
public class SalesOrderController {

    private final SaleRepository saleRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<Sale>> createSale(@RequestBody Sale sale, Authentication auth) {
        sale.setStatus(com.smarterp.modules.cashier.entity.SaleStatus.PENDING);
        return ResponseEntity.ok(ApiResponse.success(saleRepository.save(sale)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<Sale>>> getMySales(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(saleRepository.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Sale>> getSale(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(saleRepository.findById(id).orElseThrow()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Sale>> updateSale(@PathVariable String id, @RequestBody Sale sale) {
        Sale existing = saleRepository.findById(id).orElseThrow();
        existing.setSubtotal(sale.getSubtotal());
        existing.setTax(sale.getTax());
        existing.setTotal(sale.getTotal());
        return ResponseEntity.ok(ApiResponse.success(saleRepository.save(existing)));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<Sale>> confirmSale(@PathVariable String id) {
        Sale sale = saleRepository.findById(id).orElseThrow();
        sale.setStatus(com.smarterp.modules.cashier.entity.SaleStatus.COMPLETED);
        return ResponseEntity.ok(ApiResponse.success(saleRepository.save(sale)));
    }
}