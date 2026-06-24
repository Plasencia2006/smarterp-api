package com.smarterp.modules.cashier.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceSearchResponse {
    private String id;
    private String invoiceNumber;
    private String quoteNumber;
    private String customerName;
    private String customerDocument;
    private String paymentMethod;
    private BigDecimal total;
    private String status;
    private LocalDateTime paidAt;
    private String validatedBy;
    private Integer itemCount;
    private Boolean isVoided;
    private String voidReason;
}