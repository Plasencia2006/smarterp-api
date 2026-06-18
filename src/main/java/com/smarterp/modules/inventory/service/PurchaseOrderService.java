package com.smarterp.modules.inventory.service;

import com.smarterp.modules.inventory.dto.PurchaseItemRequest;
import com.smarterp.modules.inventory.dto.PurchaseOrderRequest;
import com.smarterp.modules.inventory.entity.*;
import com.smarterp.modules.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    private final StockAlertService stockAlertService; // ✅ NUEVO

    /**
     * ✨ MÉTODO MÁGICO: Recibir mercadería y actualizar stock automáticamente
     */
    @Transactional
    public PurchaseOrder receiveMerchandise(String orderId, String businessId) {
        log.info("🚚 Recibiendo mercadería de orden: {}", orderId);
        log.info("🏢 Business ID: {}", businessId);

        // 1️⃣ Buscar la orden con sus items
        PurchaseOrder order = purchaseOrderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + orderId));

        log.info("📦 Orden encontrada: {} - Items: {}", order.getOrderNumber(), order.getItems().size());

        // 2️⃣ Validar que pertenezca al negocio
        if (!order.getBusinessId().equals(businessId)) {
            log.error("❌ Business ID no coincide. Orden: {}, Request: {}",
                    order.getBusinessId(), businessId);
            throw new RuntimeException("No tiene permisos para esta orden");
        }

        // 3️⃣ Validar que esté pendiente
        if (order.getStatus() != PurchaseStatus.PENDING) {
            throw new RuntimeException("La orden ya fue " + order.getStatus());
        }

        // 4️⃣ Obtener los items de la orden
        List<PurchaseItem> items = order.getItems();

        if (items == null || items.isEmpty()) {
            throw new RuntimeException("La orden no tiene items");
        }

        log.info("📦 Procesando {} items de la orden", items.size());

        // 5️⃣ LA MAGIA: Por cada item, actualizar stock
        for (PurchaseItem item : items) {
            try {
                log.info("📦 Procesando producto: {} (ID: {}, cantidad: {})",
                        item.getProductName(), item.getProductId(), item.getQuantity());

                // Buscar o crear el stock del producto
                Stock stock = stockRepository.findByProductIdAndBusinessId(item.getProductId(), businessId)
                        .orElseGet(() -> {
                            log.info("🆕 Creando nuevo stock para producto: {}", item.getProductName());
                            return Stock.builder()
                                    .productId(item.getProductId())
                                    .productName(item.getProductName())
                                    .productSku(item.getProductSku())
                                    .businessId(businessId)
                                    .quantity(0)
                                    .minStock(5)
                                    .build();
                        });

                // ✅ SUMAR al stock actual
                int cantidadAnterior = stock.getQuantity();
                int nuevaCantidad = cantidadAnterior + item.getQuantity();
                stock.setQuantity(nuevaCantidad);

                // Guardar el stock actualizado
                stockRepository.save(stock);
                // ✅ Generar alerta automáticamente después de actualizar stock
                stockAlertService.checkAndCreateAlert(item.getProductId(), businessId);

                log.info("✅ Stock actualizado: {} → {} (+{})",
                        cantidadAnterior, nuevaCantidad, item.getQuantity());

                // 6️⃣ Registrar movimiento de stock
                StockMovement movement = StockMovement.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .businessId(businessId)
                        .type(StockMovementType.IN)
                        .quantity(item.getQuantity())
                        .reason("Recepción de orden de compra #" + order.getOrderNumber())
                        .referenceId(orderId)
                        .previousQuantity(cantidadAnterior)
                        .newQuantity(nuevaCantidad)
                        .build();

                movementRepository.save(movement);
                log.info("📝 Movimiento registrado: +{} unidades", item.getQuantity());

            } catch (Exception e) {
                log.error("❌ Error procesando item {}: {}", item.getProductId(), e.getMessage(), e);
                throw new RuntimeException("Error al actualizar stock del producto: " +
                        item.getProductName() + " - " + e.getMessage(), e);
            }
        }

        // 7️⃣ Marcar orden como RECIBIDA
        order.setStatus(PurchaseStatus.RECEIVED);
        order.setReceivedAt(LocalDateTime.now());
        PurchaseOrder updatedOrder = purchaseOrderRepository.save(order);

        log.info("✅ Orden {} marcada como RECIBIDA. {} productos actualizados.",
                order.getOrderNumber(), items.size());

        return updatedOrder;
    }

    /**
     * Crear una nueva orden de compra con sus items
     */
    @Transactional
    public PurchaseOrder createOrder(String businessId, PurchaseOrderRequest request) {
        log.info("➕ Creando orden de compra para business: {}", businessId);
        log.info("📝 Proveedor: {} ({})", request.getSupplierName(), request.getSupplierId());
        log.info("📝 Items: {}", request.getItems().size());

        // Calcular total
        BigDecimal total = request.getItems().stream()
                .map(item -> item.getUnitCost().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Crear orden
        PurchaseOrder order = PurchaseOrder.builder()
                .businessId(businessId)
                .supplierId(request.getSupplierId())
                .supplierName(request.getSupplierName())
                .notes(request.getNotes())
                .total(total)
                .status(PurchaseStatus.PENDING)
                .build();

        // Crear items y agregarlos a la orden
        for (PurchaseItemRequest itemReq : request.getItems()) {
            PurchaseItem item = PurchaseItem.builder()
                    .productId(itemReq.getProductId())
                    .productName(itemReq.getProductName())
                    .productSku(itemReq.getProductSku())
                    .quantity(itemReq.getQuantity())
                    .unitCost(itemReq.getUnitCost())
                    .build();
            order.addItem(item);
        }

        PurchaseOrder saved = purchaseOrderRepository.save(order);
        log.info("✅ Orden creada: {} con {} items, total: {}",
                saved.getOrderNumber(), saved.getItems().size(), saved.getTotal());

        return saved;
    }

    /**
     * Obtener todas las órdenes de un negocio CON sus items
     */
    @Transactional(readOnly = true)
    public List<PurchaseOrder> getOrdersByBusiness(String businessId) {
        log.info("📋 Obteniendo órdenes para business: {}", businessId);
        List<PurchaseOrder> orders = purchaseOrderRepository.findByBusinessIdWithItems(businessId);
        log.info("✅ Encontradas {} órdenes", orders.size());
        return orders;
    }

    /**
     * Obtener una orden por ID CON sus items
     */
    @Transactional(readOnly = true)
    public PurchaseOrder getOrderById(String orderId, String businessId) {
        log.info("🔍 Buscando orden: {} para business: {}", orderId, businessId);

        PurchaseOrder order = purchaseOrderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        if (!order.getBusinessId().equals(businessId)) {
            throw new RuntimeException("No tiene permisos");
        }

        return order;
    }
}