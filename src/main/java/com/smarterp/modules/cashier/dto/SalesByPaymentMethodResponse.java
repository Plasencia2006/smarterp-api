package com.smarterp.modules.cashier.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesByPaymentMethodResponse {
    private String paymentMethod;
    private Long invoiceCount;
    private BigDecimal totalAmount;
    private Double percentage;
}