package com.smarterp.modules.cashier.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class PaymentResponse {
    private String method;
    private BigDecimal amount;
    private String reference;
}