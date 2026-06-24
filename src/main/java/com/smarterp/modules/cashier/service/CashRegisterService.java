package com.smarterp.modules.cashier.service;

import com.smarterp.modules.cashier.dto.*;
import com.smarterp.modules.cashier.entity.*;
import com.smarterp.modules.cashier.repository.*;
import com.smarterp.modules.inventory.entity.Stock;
import com.smarterp.modules.inventory.entity.StockMovement;
import com.smarterp.modules.inventory.entity.StockMovementType;
import com.smarterp.modules.inventory.repository.StockRepository;
import com.smarterp.modules.inventory.repository.StockMovementRepository;
import com.smarterp.modules.sales.entity.Quote;
import com.smarterp.modules.sales.entity.QuoteItem;
import com.smarterp.modules.sales.entity.QuoteStatus;
import com.smarterp.modules.sales.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashRegisterService {

    private final CashRegisterRepository registerRepository;
    private final CashTransactionRepository transactionRepository;
    private final QuoteRepository quoteRepository;
    private final StockRepository stockRepository;
    private final ProductSerialRepository serialRepository;
    private final StockMovementRepository stockMovementRepository;

    /**
     * 🟢 APERTURA DE CAJA
     */
    @Transactional
    public CashRegister openRegister(String businessId, String userId, String userName,
            CashRegisterOpenRequest request) {
        registerRepository.findByUserIdAndStatus(userId, CashRegisterStatus.ABIERTO)
                .ifPresent(register -> {
                    throw new RuntimeException(
                            "Ya tienes un turno abierto. Ciérralo antes de abrir uno nuevo.");
                });

        LocalDateTime now = LocalDateTime.now();

        CashRegister register = CashRegister.builder()
                .businessId(businessId)
                .userId(userId)
                .userName(userName)
                .status(CashRegisterStatus.ABIERTO)
                .openingTime(now)
                .openedAt(now)
                .openedBy(userId)
                .initialAmount(request.getInitialCash() != null ? request.getInitialCash()
                        : BigDecimal.ZERO)
                .openingNotes(request.getOpeningNotes())
                .build();

        CashRegister savedRegister = registerRepository.save(register);

        CashTransaction transaction = CashTransaction.builder()
                .businessId(businessId)
                .registerId(savedRegister.getId())
                .type(TransactionType.APERTURA)
                .amount(savedRegister.getInitialAmount())
                .description("Apertura de caja - " + savedRegister.getId().substring(0, 8))
                .createdBy(userName)
                .build();

        transactionRepository.save(transaction);

        log.info("✅ Cajero {} abrió caja con S/ {}", userName, savedRegister.getInitialAmount());
        return savedRegister;
    }

    /**
     * 🔴 CIERRE DE CAJA
     */
    @Transactional
    public CashRegister closeRegister(String registerId, String businessId, String userId,
            CashRegisterCloseRequest request) {
        CashRegister register = registerRepository.findById(registerId)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado"));

        if (!register.getBusinessId().equals(businessId)) {
            throw new RuntimeException("No tiene permisos para este turno");
        }

        if (register.getStatus() != CashRegisterStatus.ABIERTO) {
            throw new RuntimeException("El turno no está abierto");
        }

        BigDecimal expectedCash = calculateExpectedCash(register);

        register.setStatus(CashRegisterStatus.CERRADO);
        register.setClosingTime(LocalDateTime.now());
        register.setFinalCash(request.getFinalCash() != null ? request.getFinalCash() : BigDecimal.ZERO);
        register.setExpectedCash(expectedCash);
        register.setCashDifference(register.getFinalCash().subtract(expectedCash));
        register.setClosingNotes(request.getClosingNotes());

        CashRegister closedRegister = registerRepository.save(register);

        CashTransaction transaction = CashTransaction.builder()
                .businessId(businessId)
                .registerId(registerId)
                .type(TransactionType.CIERRE)
                .amount(closedRegister.getFinalCash())
                .description("Cierre de caja - " + registerId.substring(0, 8))
                .createdBy(register.getUserName())
                .build();

        transactionRepository.save(transaction);

        log.info(" Cajero {} cerró caja. Esperado: S/ {}, Real: S/ {}, Diferencia: S/ {}",
                register.getUserName(), expectedCash, closedRegister.getFinalCash(),
                closedRegister.getCashDifference());

        return closedRegister;
    }

    /**
     * 🔍 BUSCAR COTIZACIÓN POR NÚMERO (para el cajero)
     */
    public QuoteSearchResponse searchQuote(String quoteNumber, String businessId) {
        log.info("🔍 Cajero buscando cotización: {}", quoteNumber);

        Quote quote = quoteRepository.findByQuoteNumber(quoteNumber)
                .orElseThrow(() -> new RuntimeException("Cotización no encontrada: " + quoteNumber));

        if (!quote.getBusinessId().equals(businessId)) {
            throw new RuntimeException("Cotización no pertenece a este negocio");
        }

        boolean isExpired = quote.isExpired();
        long remainingMinutes = quote.getRemainingMinutes();

        boolean isValidForPayment = quote.getStatus() == QuoteStatus.PENDIENTE && !isExpired;
        String validationMessage;

        if (quote.getStatus() == QuoteStatus.FACTURADA || quote.getStatus() == QuoteStatus.PAGADA) {
            validationMessage = "❌ Esta cotización ya fue facturada";
        } else if (quote.getStatus() == QuoteStatus.CANCELADA) {
            validationMessage = "❌ Esta cotización fue cancelada";
        } else if (isExpired) {
            validationMessage = "⚠️ La cotización ha expirado. Contacte al vendedor para renovarla.";
        } else {
            validationMessage = "✅ Cotización vigente. Puede procesar el pago.";
        }

        List<QuoteSearchResponse.QuoteItemDetail> itemDetails = quote.getItems().stream()
                .map(item -> {
                    Stock stock = stockRepository.findByProductIdAndBusinessId(
                            item.getProductId(), businessId).orElse(null);
                    int availableStock = stock != null ? stock.getQuantity() : 0;

                    boolean hasSerial = requiresSerialNumber(item.getProductName());

                    return QuoteSearchResponse.QuoteItemDetail.builder()
                            .productId(item.getProductId())
                            .productName(item.getProductName())
                            .productSku(item.getProductSku())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .subtotal(item.getSubtotal())
                            .availableStock(availableStock)
                            .hasSerialNumber(hasSerial)
                            .build();
                })
                .collect(Collectors.toList());

        log.info("✅ Cotización encontrada: {} - Estado: {} - Vigencia: {} min restantes",
                quoteNumber, quote.getStatus(), remainingMinutes);

        return QuoteSearchResponse.builder()
                .id(quote.getId())
                .quoteNumber(quote.getQuoteNumber())
                .customerName(quote.getCustomerName())
                .customerDocument(quote.getCustomerDocument())
                .sellerName(quote.getSellerName())
                .status(quote.getStatus().name())
                .isExpired(isExpired)
                .remainingMinutes(remainingMinutes)
                .blockedUntil(quote.getBlockedUntil())
                .createdAt(quote.getCreatedAt())
                .subtotal(quote.getSubtotal())
                .igv(quote.getIgv())
                .total(quote.getTotal())
                .items(itemDetails)
                .isValidForPayment(isValidForPayment)
                .validationMessage(validationMessage)
                .build();
    }

    /**
     * ✅ VALIDAR COTIZACIÓN ANTES DE PAGAR
     */
    public QuoteValidationResponse validateQuote(String quoteNumber, String businessId,
            Map<String, List<String>> serialNumbers) {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            Quote quote = quoteRepository.findByQuoteNumber(quoteNumber)
                    .orElseThrow(() -> new RuntimeException("Cotización no encontrada"));

            if (!quote.getBusinessId().equals(businessId)) {
                errors.add("Cotización no pertenece a este negocio");
                return buildValidationResponse(false, errors, warnings,
                        "❌ Cotización no pertenece a este negocio");
            }

            if (quote.getStatus() != QuoteStatus.PENDIENTE) {
                errors.add("La cotización ya está " + quote.getStatus());
            }

            if (quote.isExpired()) {
                errors.add("La cotización ha expirado");
            }

            for (QuoteItem item : quote.getItems()) {
                Stock stock = stockRepository.findByProductIdAndBusinessId(
                        item.getProductId(), businessId).orElse(null);

                if (stock == null) {
                    errors.add("Stock no encontrado para: " + item.getProductName());
                } else if (stock.getQuantity() < item.getQuantity()) {
                    errors.add("Stock insuficiente para: " + item.getProductName() +
                            " (Disponible: " + stock.getQuantity() +
                            ", Solicitado: " + item.getQuantity() + ")");
                }

                if (requiresSerialNumber(item.getProductName())) {
                    if (serialNumbers == null || !serialNumbers.containsKey(item.getProductId())) {
                        errors.add("Faltan números de serie para: " + item.getProductName());
                    } else {
                        List<String> serials = serialNumbers.get(item.getProductId());
                        if (serials.size() != item.getQuantity()) {
                            errors.add("Cantidad de seriales incorrecta para " +
                                    item.getProductName() + " (Esperado: "
                                    + item.getQuantity() +
                                    ", Recibido: " + serials.size() + ")");
                        }

                        for (String serial : serials) {
                            Optional<ProductSerial> existing = serialRepository
                                    .findBySerialNumber(serial);
                            if (existing.isPresent() && existing.get()
                                    .getStatus() == SerialStatus.VENDIDO) {
                                errors.add("Número de serie ya vendido: " + serial);
                            }
                        }
                    }
                }
            }

            if (quote.getRemainingMinutes() > 0 && quote.getRemainingMinutes() < 5) {
                warnings.add("La cotización expira en menos de 5 minutos");
            }

            boolean isValid = errors.isEmpty();
            String message = isValid ? " Cotización válida para procesar" : "❌ Cotización con errores";

            return QuoteValidationResponse.builder()
                    .isValid(isValid)
                    .message(message)
                    .errors(errors)
                    .warnings(warnings)
                    .build();

        } catch (Exception e) {
            errors.add(e.getMessage());
            return QuoteValidationResponse.builder()
                    .isValid(false)
                    .message("❌ Error al validar")
                    .errors(errors)
                    .warnings(warnings)
                    .build();
        }
    }

    /**
     * 💰 PROCESAR PAGO DE COTIZACIÓN
     */
    @Transactional
    public PaymentResponse processPayment(String businessId, String userId, String userName,
            PaymentRequest request) {
        log.info("💰 Procesando pago - Cotización: {} - Método: {}", request.getQuoteNumber(),
                request.getPaymentMethod());

        Quote quote = quoteRepository.findByQuoteNumber(request.getQuoteNumber())
                .orElseThrow(() -> new RuntimeException("Cotización no encontrada"));

        if (!quote.getBusinessId().equals(businessId)) {
            throw new RuntimeException("Cotización no pertenece a este negocio");
        }

        if (quote.getStatus() != QuoteStatus.PENDIENTE) {
            throw new RuntimeException("La cotización ya está " + quote.getStatus());
        }

        if (quote.isExpired()) {
            throw new RuntimeException("La cotización ha expirado. Contacte al vendedor.");
        }

        CashRegister activeRegister = registerRepository
                .findByUserIdAndStatus(userId, CashRegisterStatus.ABIERTO)
                .orElseThrow(() -> new RuntimeException(
                        "No tienes un turno de caja abierto. Abre caja primero."));

        if (request.getAmountPaid() == null || request.getAmountPaid().compareTo(quote.getTotal()) < 0) {
            throw new RuntimeException("Monto insuficiente. Total: S/ " + quote.getTotal());
        }

        validateSerialNumbers(quote, request.getSerialNumbers(), businessId);

        quote.setStatus(QuoteStatus.FACTURADA);
        quote.setPaymentMethod(request.getPaymentMethod());
        quote.setPaidAt(LocalDateTime.now());
        quote.setInvoiceNumber(quote.generateInvoiceNumber());
        quote.setValidatedAt(LocalDateTime.now());
        quote.setValidatedBy(userId);
        quote.setIsValid(true);

        for (QuoteItem item : quote.getItems()) {
            Stock stock = stockRepository.findByProductIdAndBusinessId(item.getProductId(), businessId)
                    .orElseThrow(() -> new RuntimeException(
                            "Stock no encontrado: " + item.getProductName()));

            int previousQuantity = stock.getQuantity();
            int nuevaCantidad = previousQuantity - item.getQuantity();

            if (nuevaCantidad < 0) {
                throw new RuntimeException("Stock insuficiente para: " + item.getProductName());
            }

            stock.setQuantity(nuevaCantidad);
            stockRepository.save(stock);

            StockMovement movement = StockMovement.builder()
                    .businessId(businessId)
                    .productId(item.getProductId())
                    .productName(item.getProductName())
                    .type(StockMovementType.OUT)
                    .quantity(item.getQuantity())
                    .previousQuantity(previousQuantity)
                    .newQuantity(nuevaCantidad)
                    .referenceId(quote.getId())
                    .reason("Venta - Factura " + quote.getInvoiceNumber() +
                            " - Cotización " + quote.getQuoteNumber() +
                            " - Cliente: " + quote.getCustomerName())
                    .build();

            stockMovementRepository.save(movement);

            log.info(" Stock descontado: {} ({} unidades). Stock: {} → {}",
                    item.getProductName(), item.getQuantity(), previousQuantity, nuevaCantidad);
        }

        Quote paidQuote = quoteRepository.save(quote);

        if (request.getSerialNumbers() != null) {
            linkSerialNumbersToInvoice(quote, request.getSerialNumbers(),
                    request.getImeis(), quote.getCustomerName(), businessId);
        }

        CashTransaction transaction = CashTransaction.builder()
                .businessId(businessId)
                .registerId(activeRegister.getId())
                .quoteId(quote.getId())
                .type(TransactionType.INGRESO)
                .amount(quote.getTotal())
                .paymentMethod(request.getPaymentMethod())
                .description("Factura " + quote.getInvoiceNumber() + " - Cotización "
                        + quote.getQuoteNumber())
                .customerName(quote.getCustomerName())
                .createdBy(userName)
                .build();

        transactionRepository.save(transaction);

        BigDecimal change = request.getAmountPaid().subtract(quote.getTotal());

        log.info(" Pago procesado - Factura: {} - Total: S/ {} - Método: {}",
                quote.getInvoiceNumber(), quote.getTotal(), request.getPaymentMethod());

        return PaymentResponse.builder()
                .quoteNumber(quote.getQuoteNumber())
                .customerName(quote.getCustomerName())
                .total(quote.getTotal())
                .paymentMethod(request.getPaymentMethod())
                .amountPaid(request.getAmountPaid())
                .change(change)
                .paidAt(quote.getPaidAt())
                .status("FACTURADA")
                .build();
    }

    /**
     * 🛒 PROCESAR VENTA DIRECTA (sin cotización previa)
     */
    @Transactional
    public PaymentResponse processDirectSale(String businessId, String userId, String userName,
            DirectSaleRequest request) {

        log.info(" Procesando venta directa - Cliente: {} - Items: {}",
                request.getCustomerName(), request.getItems().size());

        CashRegister activeRegister = registerRepository
                .findByUserIdAndStatus(userId, CashRegisterStatus.ABIERTO)
                .orElseThrow(() -> new RuntimeException("No tienes un turno de caja abierto"));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("No hay productos en la venta");
        }

       // Calcular totales (los precios YA INCLUYEN IGV)
        BigDecimal total = request.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calcular IGV incluido (para registro)
        BigDecimal subtotalSinIgv = total.divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP);
        BigDecimal igvIncluido = total.subtract(subtotalSinIgv);

        log.info("Venta directa - Subtotal: S/ {}, IGV incluido: S/ {}, Total: S/ {}", 
                subtotalSinIgv, igvIncluido, total);

        if (request.getAmountPaid() == null || request.getAmountPaid().compareTo(total) < 0) {
            throw new RuntimeException("Monto insuficiente. Total: S/ " + total);
        }

        // Generar número de factura ANTES del loop (para usarlo en los movimientos)
        String invoiceNumber = "F-" + System.currentTimeMillis();

        // Descontar stock y registrar movimientos
        for (DirectSaleRequest.DirectSaleItemRequest item : request.getItems()) {
            Stock stock = stockRepository.findByProductIdAndBusinessId(item.getProductId(), businessId)
                    .orElseThrow(() -> new RuntimeException("Stock no encontrado: " + item.getProductName()));

            int previousQuantity = stock.getQuantity();
            int nuevaCantidad = previousQuantity - item.getQuantity();

            if (nuevaCantidad < 0) {
                throw new RuntimeException("Stock insuficiente para: " + item.getProductName());
            }

            stock.setQuantity(nuevaCantidad);
            stockRepository.save(stock);

            //  REGISTRAR MOVIMIENTO DE STOCK (con campos correctos)
            StockMovement movement = StockMovement.builder()
                    .businessId(businessId)
                    .productId(item.getProductId())
                    .productName(item.getProductName())
                    .type(StockMovementType.OUT)
                    .quantity(item.getQuantity())
                    .previousQuantity(previousQuantity)
                    .newQuantity(nuevaCantidad)
                    .referenceId(invoiceNumber) // Usar referenceId
                    .reason("Venta directa - Factura " + invoiceNumber +
                            " - Cliente: " + request.getCustomerName())
                    .build();

            stockMovementRepository.save(movement);

            log.info(" Stock descontado: {} ({} unidades). Stock: {} → {}",
                    item.getProductName(), item.getQuantity(), previousQuantity, nuevaCantidad);
        }

        // Registrar transacción en caja
        CashTransaction transaction = CashTransaction.builder()
                .businessId(businessId)
                .registerId(activeRegister.getId())
                .type(TransactionType.INGRESO)
                .amount(total)
                .paymentMethod(request.getPaymentMethod())
                .description("Venta directa - Factura " + invoiceNumber)
                .customerName(request.getCustomerName())
                .createdBy(userName)
                .build();

        transactionRepository.save(transaction);

        BigDecimal change = request.getAmountPaid().subtract(total);

        log.info(" Venta directa procesada - Factura: {} - Total: S/ {}", invoiceNumber, total);

        return PaymentResponse.builder()
                .quoteNumber(invoiceNumber)
                .customerName(request.getCustomerName())
                .total(total)
                .paymentMethod(request.getPaymentMethod())
                .amountPaid(request.getAmountPaid())
                .change(change)
                .paidAt(LocalDateTime.now())
                .status("FACTURADA")
                .build();
    }

    /**
     * REGISTRAR EGRESO
     */
    @Transactional
    public CashTransaction registerExpense(String businessId, String userId, String userName,
            String registerId, BigDecimal amount, String description) {
        CashRegister register = registerRepository.findById(registerId)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado"));

        if (register.getStatus() != CashRegisterStatus.ABIERTO) {
            throw new RuntimeException("El turno no está abierto");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El monto debe ser mayor a cero");
        }

        CashTransaction transaction = CashTransaction.builder()
                .businessId(businessId)
                .registerId(registerId)
                .type(TransactionType.EGRESO)
                .amount(amount)
                .description(description)
                .createdBy(userName)
                .build();

        CashTransaction saved = transactionRepository.save(transaction);
        log.info("Egreso registrado: S/ {} - {}", amount, description);

        return saved;
    }

    /**
     * OBTENER TURNO ACTIVO
     */
    public CashRegister getActiveRegister(String userId) {
        return registerRepository.findByUserIdAndStatus(userId, CashRegisterStatus.ABIERTO)
                .orElse(null);
    }

    /**
     * HISTORIAL DE TURNOS
     */
    public List<CashRegister> getRegisterHistory(String businessId, String userId) {
        if (userId != null) {
            return registerRepository.findByBusinessIdAndUserIdOrderByOpeningTimeDesc(businessId, userId);
        }
        return registerRepository.findByBusinessIdOrderByOpeningTimeDesc(businessId);
    }

    /**
     * TRANSACCIONES DEL TURNO
     */
    public List<CashTransaction> getRegisterTransactions(String registerId) {
        return transactionRepository.findByRegisterIdOrderByCreatedAtDesc(registerId);
    }

    /**
     * RESUMEN DEL TURNO
     */
    public CashRegisterSummaryResponse getRegisterSummary(String registerId) {
        CashRegister register = registerRepository.findById(registerId)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado"));

        List<CashTransaction> transactions = transactionRepository
                .findByRegisterIdOrderByCreatedAtDesc(registerId);

        BigDecimal totalIngresos = transactions.stream()
                .filter(t -> t.getType() == TransactionType.INGRESO)
                .map(CashTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEgresos = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EGRESO)
                .map(CashTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalVentas = transactions.stream()
                .filter(t -> t.getType() == TransactionType.INGRESO)
                .count();

        List<CashRegisterSummaryResponse.TransactionSummary> transactionSummaries = transactions.stream()
                .map(t -> CashRegisterSummaryResponse.TransactionSummary.builder()
                        .type(t.getType().name())
                        .amount(t.getAmount())
                        .description(t.getDescription())
                        .paymentMethod(t.getPaymentMethod())
                        .createdAt(t.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return CashRegisterSummaryResponse.builder()
                .registerId(register.getId())
                .userName(register.getUserName())
                .openingTime(register.getOpeningTime())
                .closingTime(register.getClosingTime())
                .status(register.getStatus().name())
                .initialCash(register.getInitialAmount())
                .finalCash(register.getFinalCash())
                .expectedCash(register.getExpectedCash())
                .cashDifference(register.getCashDifference())
                .totalIngresos(totalIngresos)
                .totalEgresos(totalEgresos)
                .totalVentas(totalVentas)
                .transactions(transactionSummaries)
                .build();
    }

    /**
     * DASHBOARD DEL CAJERO
     */
    public Map<String, Object> getDashboard(String businessId, String userId) {
        Map<String, Object> dashboard = new HashMap<>();

        CashRegister activeRegister = getActiveRegister(userId);
        dashboard.put("activeRegister", activeRegister);

        if (activeRegister != null) {
            List<CashTransaction> transactions = getRegisterTransactions(activeRegister.getId());

            long totalVentas = transactions.stream()
                    .filter(t -> t.getType() == TransactionType.INGRESO)
                    .count();

            BigDecimal totalIngresos = transactions.stream()
                    .filter(t -> t.getType() == TransactionType.INGRESO)
                    .map(CashTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalEgresos = transactions.stream()
                    .filter(t -> t.getType() == TransactionType.EGRESO)
                    .map(CashTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            dashboard.put("totalVentas", totalVentas);
            dashboard.put("totalIngresos", totalIngresos);
            dashboard.put("totalEgresos", totalEgresos);
            dashboard.put("totalTransacciones", transactions.size());
            dashboard.put("efectivoEnCaja",
                    activeRegister.getInitialAmount().add(totalIngresos).subtract(totalEgresos));
        } else {
            dashboard.put("totalVentas", 0);
            dashboard.put("totalIngresos", BigDecimal.ZERO);
            dashboard.put("totalEgresos", BigDecimal.ZERO);
            dashboard.put("totalTransacciones", 0);
            dashboard.put("efectivoEnCaja", BigDecimal.ZERO);
        }

        return dashboard;
    }

    /**
     * 🔧 VALIDAR NÚMEROS DE SERIE
     */
    private void validateSerialNumbers(Quote quote, Map<String, List<String>> serialNumbers, String businessId) {
        if (serialNumbers == null)
            return;

        for (QuoteItem item : quote.getItems()) {
            if (requiresSerialNumber(item.getProductName())) {
                if (!serialNumbers.containsKey(item.getProductId())) {
                    throw new RuntimeException(
                            "Faltan números de serie para: " + item.getProductName());
                }

                List<String> serials = serialNumbers.get(item.getProductId());
                if (serials.size() != item.getQuantity()) {
                    throw new RuntimeException("Cantidad de seriales incorrecta para " +
                            item.getProductName());
                }

                Set<String> seenSerials = new HashSet<>();
                for (String serial : serials) {
                    if (seenSerials.contains(serial)) {
                        throw new RuntimeException("Número de serie duplicado: " + serial);
                    }
                    seenSerials.add(serial);

                    Optional<ProductSerial> existing = serialRepository.findBySerialNumber(serial);
                    if (existing.isPresent()
                            && existing.get().getStatus() == SerialStatus.VENDIDO) {
                        throw new RuntimeException("Número de serie ya vendido: " + serial);
                    }
                }
            }
        }
    }

    /**
     * 🔗 VINCULAR NÚMEROS DE SERIE A LA FACTURA
     */
    private void linkSerialNumbersToInvoice(Quote quote, Map<String, List<String>> serialNumbers,
            Map<String, List<String>> imeis,
            String customerName, String businessId) {
        for (QuoteItem item : quote.getItems()) {
            if (requiresSerialNumber(item.getProductName()) &&
                    serialNumbers.containsKey(item.getProductId())) {

                List<String> serials = serialNumbers.get(item.getProductId());
                List<String> itemImeis = imeis != null ? imeis.get(item.getProductId()) : null;

                for (int i = 0; i < serials.size(); i++) {
                    String serial = serials.get(i);
                    String imei = (itemImeis != null && i < itemImeis.size()) ? itemImeis.get(i)
                            : null;

                    Optional<ProductSerial> existing = serialRepository.findBySerialNumber(serial);

                    ProductSerial productSerial;
                    if (existing.isPresent()) {
                        productSerial = existing.get();
                    } else {
                        productSerial = ProductSerial.builder()
                                .businessId(businessId)
                                .productId(item.getProductId())
                                .serialNumber(serial)
                                .imei(imei)
                                .build();
                    }

                    productSerial.setStatus(SerialStatus.VENDIDO);
                    productSerial.setQuoteItemId(item.getId());
                    productSerial.setQuoteId(quote.getId());
                    productSerial.setCustomerName(customerName);
                    productSerial.setWarrantyStart(LocalDateTime.now());
                    productSerial.setWarrantyEnd(LocalDateTime.now().plusMonths(12));

                    serialRepository.save(productSerial);

                    log.info("🔗 Serial vinculado: {} - Producto: {} - Cliente: {} - Garantía hasta: {}",
                            serial, item.getProductName(), customerName,
                            productSerial.getWarrantyEnd());
                }
            }
        }
    }

    /**
     * 🔍 VERIFICAR SI UN PRODUCTO REQUIERE NÚMERO DE SERIE
     */
    private boolean requiresSerialNumber(String productName) {
        if (productName == null)
            return false;
        String lower = productName.toLowerCase();
        return lower.contains("celular") ||
                lower.contains("laptop") ||
                lower.contains("computadora") ||
                lower.contains("tablet") ||
                lower.contains("smartphone") ||
                lower.contains("iphone") ||
                lower.contains("samsung") ||
                lower.contains("xiaomi") ||
                lower.contains("tv") ||
                lower.contains("televisor");
    }

    /**
     *  CALCULAR EFECTIVO ESPERADO
     */
    private BigDecimal calculateExpectedCash(CashRegister register) {
        List<CashTransaction> transactions = transactionRepository
                .findByRegisterIdOrderByCreatedAtDesc(register.getId());

        BigDecimal totalIngresos = transactions.stream()
                .filter(t -> t.getType() == TransactionType.INGRESO)
                .map(CashTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEgresos = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EGRESO)
                .map(CashTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return register.getInitialAmount().add(totalIngresos).subtract(totalEgresos);
    }

    /**
     *  MÉTODO AUXILIAR: Construir respuesta de validación
     */
    private QuoteValidationResponse buildValidationResponse(boolean isValid, List<String> errors,
            List<String> warnings, String message) {
        return QuoteValidationResponse.builder()
                .isValid(isValid)
                .message(message)
                .errors(errors)
                .warnings(warnings)
                .build();
    }
} 