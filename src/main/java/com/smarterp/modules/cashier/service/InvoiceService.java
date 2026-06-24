package com.smarterp.modules.cashier.service;

import com.smarterp.modules.cashier.dto.*;
import com.smarterp.modules.cashier.entity.*;
import com.smarterp.modules.cashier.repository.*;
import com.smarterp.modules.inventory.entity.Stock;
import com.smarterp.modules.inventory.repository.StockRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final QuoteRepository quoteRepository;
    private final CashTransactionRepository transactionRepository;
    private final CashRegisterRepository registerRepository;
    private final ProductSerialRepository serialRepository;
    private final StockRepository stockRepository;

    /**
     *  LISTAR FACTURAS CON FILTROS
     */
    public List<InvoiceSearchResponse> searchInvoices(String businessId, InvoiceFilterRequest filter) {
        log.info(" Buscando facturas con filtros: {}", filter);

        List<Quote> invoices;

        // Determinar rango de fechas
        LocalDate startDate = filter.getStartDate() != null ? filter.getStartDate() : LocalDate.now();
        LocalDate endDate = filter.getEndDate() != null ? filter.getEndDate() : startDate;

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // Aplicar filtros
        if (filter.getInvoiceNumber() != null && !filter.getInvoiceNumber().isBlank()) {
            // Buscar por número de factura específico
            invoices = quoteRepository.findByInvoiceNumberAndBusinessId(
                    filter.getInvoiceNumber(), businessId)
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
        } else if (filter.getCustomerName() != null && !filter.getCustomerName().isBlank()) {
            // Buscar por nombre de cliente
            invoices = quoteRepository.findInvoicesByCustomerName(businessId, filter.getCustomerName());
            // Filtrar por fecha
            invoices = invoices.stream()
                    .filter(q -> !q.getPaidAt().isBefore(startDateTime) && !q.getPaidAt().isAfter(endDateTime))
                    .collect(Collectors.toList());
        } else {
            // Listar todas las facturas del rango
            invoices = quoteRepository.findInvoicesByBusinessIdAndDateRange(
                    businessId, startDateTime, endDateTime);
        }

        // Filtrar por método de pago si se especifica
        if (filter.getPaymentMethod() != null && !filter.getPaymentMethod().isBlank()) {
            invoices = invoices.stream()
                    .filter(q -> filter.getPaymentMethod().equals(q.getPaymentMethod()))
                    .collect(Collectors.toList());
        }

        log.info(" {} facturas encontradas", invoices.size());

        return invoices.stream()
                .map(this::mapToInvoiceSearchResponse)
                .collect(Collectors.toList());
    }

    /**
     * 🔍 OBTENER DETALLE COMPLETO DE UNA FACTURA
     */
    public InvoiceDetailResponse getInvoiceDetail(String invoiceNumber, String businessId) {
        log.info(" Obteniendo detalle de factura: {}", invoiceNumber);

        Quote quote = quoteRepository.findByInvoiceNumberAndBusinessId(invoiceNumber, businessId)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada: " + invoiceNumber));

        if (quote.getStatus() != QuoteStatus.FACTURADA) {
            throw new RuntimeException("La cotización no está facturada");
        }

        // Obtener números de serie vinculados
        List<ProductSerial> serials = serialRepository.findByQuoteId(quote.getId());

        List<InvoiceDetailResponse.InvoiceItemDetail> items = quote.getItems().stream()
                .map(item -> InvoiceDetailResponse.InvoiceItemDetail.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .productSku(item.getProductSku())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        List<InvoiceDetailResponse.SerialDetail> serialDetails = serials.stream()
                .map(serial -> InvoiceDetailResponse.SerialDetail.builder()
                        .serialNumber(serial.getSerialNumber())
                        .imei(serial.getImei())
                        .productName(getProductNameFromQuote(quote, serial.getProductId()))
                        .warrantyStart(serial.getWarrantyStart())
                        .warrantyEnd(serial.getWarrantyEnd())
                        .build())
                .collect(Collectors.toList());

        return InvoiceDetailResponse.builder()
                .id(quote.getId())
                .invoiceNumber(quote.getInvoiceNumber())
                .quoteNumber(quote.getQuoteNumber())
                .issuedAt(quote.getPaidAt())
                .status(quote.getStatus().name())
                .customerName(quote.getCustomerName())
                .customerDocument(quote.getCustomerDocument())
                .cashierName(quote.getValidatedBy())
                .sellerName(quote.getSellerName())
                .paymentMethod(quote.getPaymentMethod())
                .subtotal(quote.getSubtotal())
                .igv(quote.getIgv())
                .tax(quote.getTax())
                .total(quote.getTotal())
                .items(items)
                .serials(serialDetails)
                .isVoided(false)
                .build();
    }

    /**
     * 🚫 ANULAR FACTURA (con reversión de stock)
     */
    @Transactional
    public InvoiceDetailResponse voidInvoice(String invoiceNumber, String businessId,
            String userId, String userName,
            VoidInvoiceRequest request) {
        log.info("🚫 Anulando factura: {} - Motivo: {}", invoiceNumber, request.getReason());

        Quote quote = quoteRepository.findByInvoiceNumberAndBusinessId(invoiceNumber, businessId)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada: " + invoiceNumber));

        if (quote.getStatus() != QuoteStatus.FACTURADA) {
            throw new RuntimeException("Solo se pueden anular facturas facturadas");
        }

        // Validar que no haya pasado mucho tiempo (máximo 24 horas)
        if (quote.getPaidAt() != null) {
            long hoursSincePayment = java.time.Duration.between(quote.getPaidAt(), LocalDateTime.now()).toHours();
            if (hoursSincePayment > 24) {
                throw new RuntimeException("No se puede anular una factura con más de 24 horas de emitida");
            }
        }

        //  REVERTIR STOCK si se solicita
        if (request.getRestoreStock() == null || request.getRestoreStock()) {
            for (QuoteItem item : quote.getItems()) {
                Stock stock = stockRepository.findByProductIdAndBusinessId(
                        item.getProductId(), businessId).orElse(null);

                if (stock != null) {
                    int nuevaCantidad = stock.getQuantity() + item.getQuantity();
                    stock.setQuantity(nuevaCantidad);
                    stockRepository.save(stock);

                    log.info(" Stock restaurado: {} (+{} unidades). Nuevo stock: {}",
                            item.getProductName(), item.getQuantity(), nuevaCantidad);
                }
            }
        }

        //  LIBERAR NÚMEROS DE SERIE
        List<ProductSerial> serials = serialRepository.findByQuoteId(quote.getId());
        for (ProductSerial serial : serials) {
            serial.setStatus(SerialStatus.DISPONIBLE);
            serial.setQuoteId(null);
            serial.setQuoteItemId(null);
            serial.setCustomerName(null);
            serial.setWarrantyStart(null);
            serial.setWarrantyEnd(null);
            serialRepository.save(serial);

            log.info("🔓 Serial liberado: {}", serial.getSerialNumber());
        }

        //  CAMBIAR ESTADO A CANCELADA
        quote.setStatus(QuoteStatus.CANCELADA);
        quote.setNotes("ANULADA - " + request.getReason() + " (por " + userName + ")");
        quoteRepository.save(quote);

        //  REGISTRAR TRANSACCIÓN DE EGRESO (reversión)
        CashRegister activeRegister = registerRepository.findByUserIdAndStatus(
                userId, CashRegisterStatus.ABIERTO).orElse(null);

        if (activeRegister != null) {
            CashTransaction transaction = CashTransaction.builder()
                    .businessId(businessId)
                    .registerId(activeRegister.getId())
                    .quoteId(quote.getId())
                    .type(TransactionType.EGRESO)
                    .amount(quote.getTotal())
                    .paymentMethod(quote.getPaymentMethod())
                    .description("ANULACIÓN Factura " + invoiceNumber + " - " + request.getReason())
                    .customerName(quote.getCustomerName())
                    .createdBy(userName)
                    .build();

            transactionRepository.save(transaction);
        }

        log.info(" Factura {} anulada correctamente", invoiceNumber);

        return getInvoiceDetail(invoiceNumber, businessId);
    }

    /**
     *  RESUMEN DE VENTAS DEL DÍA
     */
    public DailySalesSummaryResponse getDailySummary(String businessId, LocalDate date) {
        log.info(" Generando resumen de ventas del día: {}", date);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<Quote> invoices = quoteRepository.findInvoicesByBusinessIdAndDateRange(
                businessId, startOfDay, endOfDay);

        Long totalInvoices = (long) invoices.size();
        BigDecimal totalSales = invoices.stream()
                .map(Quote::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageTicket = totalInvoices > 0
                ? totalSales.divide(BigDecimal.valueOf(totalInvoices), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Ventas por método de pago
        Map<String, List<Quote>> byPaymentMethod = invoices.stream()
                .filter(q -> q.getPaymentMethod() != null)
                .collect(Collectors.groupingBy(Quote::getPaymentMethod));

        List<SalesByPaymentMethodResponse> salesByPayment = byPaymentMethod.entrySet().stream()
                .map(entry -> {
                    Long count = (long) entry.getValue().size();
                    BigDecimal total = entry.getValue().stream()
                            .map(Quote::getTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    Double percentage = totalSales.compareTo(BigDecimal.ZERO) > 0
                            ? total.multiply(BigDecimal.valueOf(100))
                                    .divide(totalSales, 2, RoundingMode.HALF_UP)
                                    .doubleValue()
                            : 0.0;

                    return SalesByPaymentMethodResponse.builder()
                            .paymentMethod(entry.getKey())
                            .invoiceCount(count)
                            .totalAmount(total)
                            .percentage(percentage)
                            .build();
                })
                .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
                .collect(Collectors.toList());

        // Ventas por hora
        Map<Integer, List<Quote>> byHour = invoices.stream()
                .filter(q -> q.getPaidAt() != null)
                .collect(Collectors.groupingBy(q -> q.getPaidAt().getHour()));

        List<DailySalesSummaryResponse.HourlySalesResponse> hourlySales = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            List<Quote> hourInvoices = byHour.getOrDefault(hour, Collections.emptyList());
            hourlySales.add(DailySalesSummaryResponse.HourlySalesResponse.builder()
                    .hour(hour)
                    .invoiceCount((long) hourInvoices.size())
                    .totalAmount(hourInvoices.stream()
                            .map(Quote::getTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add))
                    .build());
        }

        // Producto más vendido
        Map<String, Long> productCount = new HashMap<>();
        Map<String, String> productNames = new HashMap<>();

        for (Quote invoice : invoices) {
            for (QuoteItem item : invoice.getItems()) {
                productCount.merge(item.getProductId(), (long) item.getQuantity(), Long::sum);
                productNames.put(item.getProductId(), item.getProductName());
            }
        }

        String topProductId = productCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        String topProductName = topProductId != null ? productNames.get(topProductId) : "N/A";
        Long topProductQuantity = topProductId != null ? productCount.get(topProductId) : 0L;

        return DailySalesSummaryResponse.builder()
                .date(date.toString())
                .totalInvoices(totalInvoices)
                .totalSales(totalSales)
                .averageTicket(averageTicket)
                .salesByPaymentMethod(salesByPayment)
                .hourlySales(hourlySales)
                .topProduct(topProductName)
                .topProductQuantity(topProductQuantity)
                .build();
    }

    /**
     *  VENTAS AGRUPADAS POR MÉTODO DE PAGO
     */
    public List<SalesByPaymentMethodResponse> getSalesByPaymentMethod(
            String businessId, LocalDate startDate, LocalDate endDate) {

        log.info(" Obteniendo ventas por método de pago: {} a {}", startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Quote> invoices = quoteRepository.findInvoicesByBusinessIdAndDateRange(
                businessId, startDateTime, endDateTime);

        BigDecimal totalSales = invoices.stream()
                .map(Quote::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, List<Quote>> byPaymentMethod = invoices.stream()
                .filter(q -> q.getPaymentMethod() != null)
                .collect(Collectors.groupingBy(Quote::getPaymentMethod));

        return byPaymentMethod.entrySet().stream()
                .map(entry -> {
                    Long count = (long) entry.getValue().size();
                    BigDecimal total = entry.getValue().stream()
                            .map(Quote::getTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    Double percentage = totalSales.compareTo(BigDecimal.ZERO) > 0
                            ? total.multiply(BigDecimal.valueOf(100))
                                    .divide(totalSales, 2, RoundingMode.HALF_UP)
                                    .doubleValue()
                            : 0.0;

                    return SalesByPaymentMethodResponse.builder()
                            .paymentMethod(entry.getKey())
                            .invoiceCount(count)
                            .totalAmount(total)
                            .percentage(percentage)
                            .build();
                })
                .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
                .collect(Collectors.toList());
    }

    /**
     *  BUSCAR FACTURA POR NÚMERO
     */
    public InvoiceSearchResponse findInvoiceByNumber(String invoiceNumber, String businessId) {
        Quote quote = quoteRepository.findByInvoiceNumberAndBusinessId(invoiceNumber, businessId)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada: " + invoiceNumber));

        return mapToInvoiceSearchResponse(quote);
    }

    /**
     * 🔧 MÉTODO AUXILIAR: Mapear Quote a InvoiceSearchResponse
     */
    private InvoiceSearchResponse mapToInvoiceSearchResponse(Quote quote) {
        boolean isVoided = quote.getStatus() == QuoteStatus.CANCELADA &&
                quote.getNotes() != null &&
                quote.getNotes().startsWith("ANULADA");

        String voidReason = isVoided && quote.getNotes() != null
                ? quote.getNotes().replace("ANULADA - ", "")
                : null;

        return InvoiceSearchResponse.builder()
                .id(quote.getId())
                .invoiceNumber(quote.getInvoiceNumber())
                .quoteNumber(quote.getQuoteNumber())
                .customerName(quote.getCustomerName())
                .customerDocument(quote.getCustomerDocument())
                .paymentMethod(quote.getPaymentMethod())
                .total(quote.getTotal())
                .status(quote.getStatus().name())
                .paidAt(quote.getPaidAt())
                .validatedBy(quote.getValidatedBy())
                .itemCount(quote.getItems() != null ? quote.getItems().size() : 0)
                .isVoided(isVoided)
                .voidReason(voidReason)
                .build();
    }

    /**
     * 🔧 MÉTODO AUXILIAR: Obtener nombre de producto desde Quote
     */
    private String getProductNameFromQuote(Quote quote, String productId) {
        return quote.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .map(QuoteItem::getProductName)
                .findFirst()
                .orElse("Producto");
    }
}