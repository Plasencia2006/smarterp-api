package com.smarterp.modules.cashier.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDetailResponse {
    // Datos de la factura
    private String id;
    private String invoiceNumber;
    private String quoteNumber;
    private LocalDateTime issuedAt;
    private String status;

    // Datos del cliente
    private String customerName;
    private String customerDocument;

    // Datos del cajero
    private String cashierName;
    private String sellerName;

    // Datos del pago
    private String paymentMethod;
    private BigDecimal subtotal;
    private BigDecimal igv;
    private BigDecimal tax;
    private BigDecimal total;

    // Items
    private List<InvoiceItemDetail> items;

    // Números de serie (para garantías)
    private List<SerialDetail> serials;

    // Estado de anulación
    private Boolean isVoided;
    private String voidReason;
    private LocalDateTime voidedAt;
    private String voidedBy;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InvoiceItemDetail {
        private String productId;
        private String productName;
        private String productSku;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SerialDetail {
        private String serialNumber;
        private String imei;
        private String productName;
        private LocalDateTime warrantyStart;
        private LocalDateTime warrantyEnd;
    }
}