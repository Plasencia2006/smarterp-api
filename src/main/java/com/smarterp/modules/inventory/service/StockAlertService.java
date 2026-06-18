package com.smarterp.modules.inventory.service;

import com.smarterp.modules.inventory.entity.Stock;
import com.smarterp.modules.inventory.entity.StockAlert;
import com.smarterp.modules.inventory.repository.StockAlertRepository;
import com.smarterp.modules.inventory.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockAlertService {

    private final StockAlertRepository alertRepository;
    private final StockRepository stockRepository;

    /**
     * ✅ Generar alertas automáticamente para un producto específico
     * Se llama después de cada cambio de stock
     */
    @Transactional
    public void checkAndCreateAlert(String productId, String businessId) {
        Stock stock = stockRepository.findByProductIdAndBusinessId(productId, businessId)
                .orElse(null);

        if (stock == null)
            return;

        String alertType;
        String message;

        if (stock.getQuantity() <= 0) {
            alertType = "OUT_OF_STOCK";
            message = "Producto sin stock. Stock actual: 0";
        } else if (stock.getQuantity() <= stock.getMinStock()) {
            alertType = "LOW_STOCK";
            message = "Stock bajo. Actual: " + stock.getQuantity() +
                    " / Mínimo: " + stock.getMinStock();
        } else {
            // ✅ Stock OK - Eliminar alertas pendientes si existen
            alertRepository.findByProductIdAndBusinessIdAndIsAttended(
                    productId, businessId, false)
                    .ifPresent(alert -> {
                        alert.setIsAttended(true);
                        alert.setAttendedAt(LocalDateTime.now());
                        alertRepository.save(alert);
                        log.info("✅ Alerta resuelta automáticamente para: {}", stock.getProductName());
                    });
            return;
        }

        // ✅ Crear o actualizar alerta
        alertRepository.findByProductIdAndBusinessIdAndIsAttended(
                productId, businessId, false)
                .ifPresentOrElse(alert -> {
                    // Actualizar alerta existente
                    alert.setCurrentStock(stock.getQuantity());
                    alert.setMinStock(stock.getMinStock());
                    alert.setAlertType(alertType);
                    alert.setMessage(message);
                    alertRepository.save(alert);
                    log.info(" Alerta actualizada: {} ({})", stock.getProductName(), alertType);
                }, () -> {
                    // Crear nueva alerta
                    StockAlert newAlert = StockAlert.builder()
                            .productId(productId)
                            .productName(stock.getProductName())
                            .productSku(stock.getProductSku())
                            .businessId(businessId)
                            .currentStock(stock.getQuantity())
                            .minStock(stock.getMinStock())
                            .alertType(alertType)
                            .isAttended(false)
                            .message(message)
                            .build();
                    alertRepository.save(newAlert);
                    log.info("🆕 Alerta creada: {} ({})", stock.getProductName(), alertType);
                });
    }

    /**
     * ✅ Generar alertas para todos los productos del negocio
     */
    @Transactional
    public int generateAllAlerts(String businessId) {
        log.info("🔔 Generando alertas para todos los productos del business: {}", businessId);

        List<Stock> allStocks = stockRepository.findByBusinessId(businessId);
        int alertsCreated = 0;

        for (Stock stock : allStocks) {
            try {
                checkAndCreateAlert(stock.getProductId(), businessId);
                alertsCreated++;
            } catch (Exception e) {
                log.error("Error generando alerta para producto {}: {}",
                        stock.getProductId(), e.getMessage());
            }
        }

        log.info("✅ {} alertas procesadas", alertsCreated);
        return alertsCreated;
    }

    /**
     * ✅ Obtener estadísticas de alertas
     */
    public long getPendingAlertsCount(String businessId) {
        return alertRepository.findByBusinessIdAndIsAttended(businessId, false).size();
    }

    public long getAttendedAlertsCount(String businessId) {
        return alertRepository.findByBusinessIdAndIsAttended(businessId, true).size();
    }

    public long getTotalAlertsCount(String businessId) {
        return alertRepository.findByBusinessIdOrderByCreatedAtDesc(businessId).size();
    }
}