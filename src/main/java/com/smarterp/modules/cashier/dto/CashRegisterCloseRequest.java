package com.smarterp.modules.cashier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CashRegisterCloseRequest {
    @NotBlank(message = "El ID de la caja es obligatorio")
    private String id;

    @NotNull(message = "El monto final es obligatorio")
    private BigDecimal finalAmount;
}