package com.smarterp.modules.cashier.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private String quoteNumber;
    private String customerName;
    private BigDecimal total;
    private String paymentMethod;
    private BigDecimal amountPaid;
    private BigDecimal change;
    private LocalDateTime paidAt;
    private String status;
}