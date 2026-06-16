package com.smarterp.modules.cashier.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CashRegisterOpenRequest {
    @NotNull(message = "El monto inicial es obligatorio")
    private BigDecimal initialAmount;
}