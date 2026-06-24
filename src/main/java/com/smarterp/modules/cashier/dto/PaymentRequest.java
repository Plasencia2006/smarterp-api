package com.smarterp.modules.cashier.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {
    private String quoteNumber;
    private String paymentMethod;
    private BigDecimal amountPaid;
    private String notes;
    private Map<String, List<String>> serialNumbers;
    private Map<String, List<String>> imeis;
}