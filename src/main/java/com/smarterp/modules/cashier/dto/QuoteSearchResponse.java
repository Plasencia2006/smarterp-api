package com.smarterp.modules.cashier.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuoteSearchResponse {
    private String id;
    private String quoteNumber;
    private String invoiceNumber; // ✅ NUEVO - Para facturas
    private String customerName;
    private String customerDocument;
    private String sellerName;
    private String status;

    private Boolean isExpired;
    private Long remainingMinutes;
    private LocalDateTime blockedUntil;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt; // ✅ NUEVO - Para facturas
    private String validatedBy; // ✅ NUEVO - Para facturas
    private String paymentMethod; // ✅ NUEVO - Para facturas

    private BigDecimal subtotal;
    private BigDecimal igv;
    private BigDecimal total;

    private List<QuoteItemDetail> items;

    private Boolean isValidForPayment;
    private String validationMessage;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuoteItemDetail {
        private String productId;
        private String productName;
        private String productSku;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
        private Integer availableStock;
        private Boolean hasSerialNumber;
    }
}