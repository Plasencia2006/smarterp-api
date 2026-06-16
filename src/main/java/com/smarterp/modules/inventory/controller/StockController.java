package com.smarterp.modules.inventory.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.inventory.entity.Stock;
import com.smarterp.modules.inventory.entity.StockMovement;
import com.smarterp.modules.inventory.repository.StockMovementRepository;
import com.smarterp.modules.inventory.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    private final UserContext userContext;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Stock>>> getStock() {
        String businessId = userContext.getCurrentBusinessId();
        return ResponseEntity.ok(ApiResponse.success(stockRepository.findByBusinessId(businessId)));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<Stock>> getStockByProduct(@PathVariable String productId) {
        String businessId = userContext.getCurrentBusinessId();
        Stock stock = stockRepository.findByProductIdAndBusinessId(productId, businessId).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(stock));
    }

    @PostMapping("/adjust")
    public ResponseEntity<ApiResponse<StockMovement>> adjustStock(@RequestBody StockMovement movement) {
        return ResponseEntity.ok(ApiResponse.success("Ajuste registrado", movementRepository.save(movement)));
    }

    @GetMapping("/movements")
    public ResponseEntity<ApiResponse<List<StockMovement>>> getMovements() {
        return ResponseEntity.ok(ApiResponse.success(movementRepository.findAll()));
    }
}