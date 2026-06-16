package com.smarterp.modules.cashier.service;

import com.smarterp.common.exceptions.BusinessException;
import com.smarterp.common.exceptions.ResourceNotFoundException;
import com.smarterp.modules.cashier.dto.*;
import com.smarterp.modules.cashier.entity.*;
import com.smarterp.modules.cashier.repository.*;
import com.smarterp.modules.inventory.entity.Product;
import com.smarterp.modules.inventory.entity.Stock;
import com.smarterp.modules.inventory.repository.ProductRepository;
import com.smarterp.modules.inventory.repository.StockRepository;
import com.smarterp.shared.entity.User;
import com.smarterp.shared.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaleService {

    private final SaleRepository saleRepository;
    private final PaymentRepository paymentRepository;
    private final CashRegisterService cashRegisterService;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final UserRepository userRepository;

    /**
     * Crea una nueva venta
     * 
     * @param businessId ID del negocio (del token JWT)
     * @param userId     ID del usuario cajero (del token JWT)
     * @param request    Datos de la venta
     */
    @Transactional
    public SaleResponse createSale(String businessId, String userId, SaleRequest request) {
        User cashier = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));

        CashRegister cashRegister = cashRegisterService.getOpenCashRegister(businessId);

        // Calcular totales
        BigDecimal subtotal = BigDecimal.ZERO;
        List<SaleItem> saleItems = new ArrayList<>();

        for (SaleItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

            // Validar stock
            Stock stock = stockRepository.findByProductIdAndBusinessId(product.getId(), businessId)
                    .orElseThrow(() -> new BusinessException("Stock no encontrado para " + product.getName()));

            if (stock.getQuantity() < itemReq.getQuantity()) {
                throw new BusinessException("Stock insuficiente para " + product.getName());
            }

            BigDecimal itemSubtotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            subtotal = subtotal.add(itemSubtotal);

            SaleItem saleItem = SaleItem.builder()
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .price(product.getPrice())
                    .subtotal(itemSubtotal)
                    .build();
            saleItems.add(saleItem);

            // Descontar stock
            stock.setQuantity(stock.getQuantity() - itemReq.getQuantity());
            stockRepository.save(stock);
        }

        BigDecimal tax = subtotal.multiply(new BigDecimal("0.18"));
        BigDecimal total = subtotal.add(tax);

        Sale sale = Sale.builder()
                .business(cashRegister.getBusiness())
                .cashier(cashier)
                .cashRegister(cashRegister)
                .subtotal(subtotal)
                .tax(tax)
                .total(total)
                .status(SaleStatus.COMPLETED)
                .build();

        saleItems.forEach(item -> item.setSale(sale));
        sale.setItems(saleItems);

        final Sale savedSale = saleRepository.save(sale);

        // Procesar pagos
        if (request.getPayments() != null) {
            for (PaymentRequest payReq : request.getPayments()) {
                Payment payment = Payment.builder()
                        .sale(savedSale)
                        .method(PaymentMethod.valueOf(payReq.getMethod()))
                        .amount(payReq.getAmount())
                        .reference(payReq.getReference())
                        .build();
                paymentRepository.save(payment);
            }
        }

        log.info("Venta creada: {} por cajero: {} en negocio: {}", savedSale.getId(), userId, businessId);
        return mapToResponse(savedSale);
    }

    /**
     * Obtiene las ventas del cajero en la caja actual
     * 
     * @param businessId ID del negocio (del token JWT)
     * @param userId     ID del usuario cajero (del token JWT)
     */
    public List<SaleResponse> getMySales(String businessId, String userId) {
        CashRegister cashRegister = cashRegisterService.getOpenCashRegister(businessId);

        return saleRepository.findByCashierIdAndCashRegisterId(
                userId,
                cashRegister.getId(),
                PageRequest.of(0, 100))
                .getContent()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una venta por ID, validando que pertenezca al negocio
     * 
     * @param id         ID de la venta
     * @param businessId ID del negocio (del token JWT)
     */
    public SaleResponse getSaleById(String id, String businessId) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada"));

        // Validar que la venta pertenezca al negocio
        if (!sale.getBusiness().getId().equals(businessId)) {
            throw new BusinessException("No tiene permisos para ver esta venta");
        }

        return mapToResponse(sale);
    }

    /**
     * Cancela una venta, restaurando el stock
     * 
     * @param id         ID de la venta
     * @param businessId ID del negocio (del token JWT)
     */
    @Transactional
    public SaleResponse cancelSale(String id, String businessId) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada"));

        // Validar que la venta pertenezca al negocio
        if (!sale.getBusiness().getId().equals(businessId)) {
            throw new BusinessException("No tiene permisos para cancelar esta venta");
        }

        if (sale.getStatus() == SaleStatus.CANCELLED) {
            throw new BusinessException("La venta ya está cancelada");
        }

        // Restaurar stock
        for (SaleItem item : sale.getItems()) {
            Stock stock = stockRepository.findByProductIdAndBusinessId(
                    item.getProduct().getId(), businessId).orElse(null);
            if (stock != null) {
                stock.setQuantity(stock.getQuantity() + item.getQuantity());
                stockRepository.save(stock);
            }
        }

        sale.setStatus(SaleStatus.CANCELLED);
        sale = saleRepository.save(sale);

        log.info("Venta cancelada: {} en negocio: {}", id, businessId);
        return mapToResponse(sale);
    }

    private SaleResponse mapToResponse(Sale sale) {
        return SaleResponse.builder()
                .id(sale.getId())
                .customerName(sale.getCustomer() != null ? sale.getCustomer().getName() : "Consumidor Final")
                .cashierName(sale.getCashier().getFirstName() + " " + sale.getCashier().getLastName())
                .subtotal(sale.getSubtotal())
                .tax(sale.getTax())
                .total(sale.getTotal())
                .status(sale.getStatus().name())
                .items(sale.getItems().stream().map(item -> SaleItemResponse.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .subtotal(item.getSubtotal())
                        .build()).toList())
                .createdAt(sale.getCreatedAt())
                .build();
    }
}