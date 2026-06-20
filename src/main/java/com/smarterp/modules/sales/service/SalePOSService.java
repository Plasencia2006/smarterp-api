package com.smarterp.modules.sales.service;

import com.smarterp.modules.inventory.entity.*;
import com.smarterp.modules.inventory.repository.*;
import com.smarterp.modules.inventory.service.StockAlertService;
import com.smarterp.modules.sales.dto.SaleItemRequest;
import com.smarterp.modules.sales.dto.SaleRequest;
import com.smarterp.modules.sales.entity.*;
import com.smarterp.modules.sales.repository.SalePOSRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalePOSService {

    private final SalePOSRepository salePOSRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    private final StockAlertService stockAlertService;

    @Transactional
    public SalePOS createSale(String businessId, SaleRequest request) {
        log.info("🛒 Creando venta POS - Items: {} | Pago: {}",
                request.getItems().size(), request.getPaymentMethod());

        // Validar stock
        for (SaleItemRequest itemReq : request.getItems()) {
            Stock stock = stockRepository.findByProductIdAndBusinessId(
                    itemReq.getProductId(), businessId)
                    .orElseThrow(() -> new RuntimeException(
                            "Stock no encontrado: " + itemReq.getProductName()));

            if (stock.getQuantity() < itemReq.getQuantity()) {
                throw new RuntimeException(
                        "Stock insuficiente: " + itemReq.getProductName() +
                                " (Disponible: " + stock.getQuantity() + ")");
            }
        }

        // Crear venta
        SalePOS salePOS = SalePOS.builder()
                .businessId(businessId)
                .voucherType(request.getVoucherType() != null ? request.getVoucherType() : "BOLETA")
                .customerName(request.getCustomerName())
                .customerDocument(request.getCustomerDocument())
                .paymentMethod(request.getPaymentMethod())
                .discount(request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO)
                .amountPaid(request.getAmountPaid())
                .notes(request.getNotes())
                .sellerName(request.getSellerName())
                .status(SaleStatus.COMPLETED)
                .build();

        // Agregar items
        for (SaleItemRequest itemReq : request.getItems()) {
            SalePOSItem item = SalePOSItem.builder()
                    .productId(itemReq.getProductId())
                    .productName(itemReq.getProductName())
                    .productSku(itemReq.getProductSku())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .costPrice(itemReq.getCostPrice())
                    .discount(itemReq.getDiscount() != null ? itemReq.getDiscount() : BigDecimal.ZERO)
                    .build();

            item.calculateSubtotal();
            salePOS.addItem(item);
        }

        salePOS.calculateTotals();
        SalePOS savedSale = salePOSRepository.save(salePOS);

        // Descontar stock
        for (SalePOSItem item : savedSale.getItems()) {
            Stock stock = stockRepository.findByProductIdAndBusinessId(
                    item.getProductId(), businessId).orElseThrow();

            int cantidadAnterior = stock.getQuantity();
            stock.setQuantity(cantidadAnterior - item.getQuantity());
            stockRepository.save(stock);

            StockMovement movement = StockMovement.builder()
                    .productId(item.getProductId())
                    .productName(item.getProductName())
                    .businessId(businessId)
                    .type(StockMovementType.OUT)
                    .quantity(item.getQuantity())
                    .reason("Venta POS #" + savedSale.getSaleNumber())
                    .referenceId(savedSale.getId())
                    .previousQuantity(cantidadAnterior)
                    .newQuantity(stock.getQuantity())
                    .build();

            movementRepository.save(movement);
            stockAlertService.checkAndCreateAlert(item.getProductId(), businessId);
        }

        log.info("✅ Venta POS creada: {} - Total: S/ {}", savedSale.getSaleNumber(), savedSale.getTotal());
        return savedSale;
    }

    public List<SalePOS> getSalesByBusiness(String businessId) {
        return salePOSRepository.findByBusinessIdOrderByCreatedAtDesc(businessId);
    }

    public SalePOS getSaleById(String saleId, String businessId) {
        SalePOS salePOS = salePOSRepository.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));
        if (!salePOS.getBusinessId().equals(businessId)) {
            throw new RuntimeException("No tiene permisos");
        }
        return salePOS;
    }

    @Transactional
    public SalePOS cancelSale(String saleId, String businessId) {
        SalePOS salePOS = salePOSRepository.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

        if (!salePOS.getBusinessId().equals(businessId)) {
            throw new RuntimeException("No tiene permisos");
        }

        if (salePOS.getStatus() == SaleStatus.CANCELLED) {
            throw new RuntimeException("La venta ya está cancelada");
        }

        // Restaurar stock
        for (SalePOSItem item : salePOS.getItems()) {
            Stock stock = stockRepository.findByProductIdAndBusinessId(
                    item.getProductId(), businessId).orElse(null);

            if (stock != null) {
                int cantidadAnterior = stock.getQuantity();
                stock.setQuantity(cantidadAnterior + item.getQuantity());
                stockRepository.save(stock);

                StockMovement movement = StockMovement.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .businessId(businessId)
                        .type(StockMovementType.IN)
                        .quantity(item.getQuantity())
                        .reason("Devolución - Venta POS cancelada #" + salePOS.getSaleNumber())
                        .referenceId(saleId)
                        .previousQuantity(cantidadAnterior)
                        .newQuantity(stock.getQuantity())
                        .build();

                movementRepository.save(movement);
            }
        }

        salePOS.setStatus(SaleStatus.CANCELLED);
        return salePOSRepository.save(salePOS);
    }
}