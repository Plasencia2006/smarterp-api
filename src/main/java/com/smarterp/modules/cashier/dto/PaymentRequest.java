package com.smarterp.modules.cashier.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentRequest {
    @NotNull
    private String method;
    @NotNull
    private BigDecimal amount;
    private String reference;
}