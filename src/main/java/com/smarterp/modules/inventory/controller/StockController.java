package com.smarterp.modules.inventory.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.inventory.entity.Stock;
import com.smarterp.modules.inventory.entity.StockMovement;
import com.smarterp.modules.inventory.entity.StockMovementType;
import com.smarterp.modules.inventory.repository.ProductRepository;
import com.smarterp.modules.inventory.repository.StockMovementRepository;
import com.smarterp.modules.inventory.repository.StockRepository;
import com.smarterp.modules.inventory.service.StockAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/inventory/stock")
@RequiredArgsConstructor
@Slf4j
public class StockController {

    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final StockAlertService stockAlertService; // ✅ NUEVO
    private final UserContext userContext;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Stock>>> getStock(
            @RequestParam(required = false) String search) {
        String businessId = userContext.getCurrentBusinessId();

        List<Stock> stocks;
        if (search != null && !search.isBlank()) {
            stocks = stockRepository.searchStock(businessId, search);
        } else {
            stocks = stockRepository.findByBusinessId(businessId);
        }

        return ResponseEntity.ok(ApiResponse.success(stocks));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<Stock>> getStockByProduct(@PathVariable String productId) {
        String businessId = userContext.getCurrentBusinessId();
        Stock stock = stockRepository.findByProductIdAndBusinessId(productId, businessId).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(stock));
    }

    /**
     * ✅ AJUSTAR STOCK con generación automática de alertas
     */
    @PostMapping("/adjust")
    public ResponseEntity<ApiResponse<StockMovement>> adjustStock(@RequestBody StockMovement movement) {
        String businessId = userContext.getCurrentBusinessId();

        log.info("🔄 Ajuste de stock - Tipo: {}, Producto: {}, Cantidad: {}",
                movement.getType(), movement.getProductId(), movement.getQuantity());

        Stock stock = stockRepository.findByProductIdAndBusinessId(
                movement.getProductId(), businessId)
                .orElseThrow(() -> new RuntimeException("Stock no encontrado"));

        int cantidadAnterior = stock.getQuantity();
        int nuevaCantidad;

        if (movement.getType() == StockMovementType.IN) {
            nuevaCantidad = cantidadAnterior + movement.getQuantity();
        } else if (movement.getType() == StockMovementType.OUT) {
            if (cantidadAnterior < movement.getQuantity()) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("Stock insuficiente. Actual: " + cantidadAnterior));
            }
            nuevaCantidad = cantidadAnterior - movement.getQuantity();
        } else {
            nuevaCantidad = movement.getQuantity();
        }

        stock.setQuantity(nuevaCantidad);
        stockRepository.save(stock);

        // ✅ Generar alerta automáticamente
        stockAlertService.checkAndCreateAlert(movement.getProductId(), businessId);

        movement.setBusinessId(businessId);
        movement.setProductName(stock.getProductName());
        movement.setPreviousQuantity(cantidadAnterior);
        movement.setNewQuantity(nuevaCantidad);
        movement.setCreatedAt(LocalDateTime.now());

        StockMovement saved = movementRepository.save(movement);

        log.info("✅ Stock ajustado y alertas verificadas");

        return ResponseEntity.ok(ApiResponse.success("Stock ajustado correctamente", saved));
    }

    @GetMapping("/movements")
    public ResponseEntity<ApiResponse<List<StockMovement>>> getMovements(
            @RequestParam(defaultValue = "10") int limit) {
        String businessId = userContext.getCurrentBusinessId();
        return ResponseEntity.ok(ApiResponse.success(
                movementRepository.findTop10ByBusinessIdOrderByCreatedAtDesc(businessId)));
    }
}