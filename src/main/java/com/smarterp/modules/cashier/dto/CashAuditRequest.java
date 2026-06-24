package com.smarterp.modules.cashier.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashAuditRequest {
    private String registerId;
    private String auditType; // PARCIAL, CIERRE, SORPRESA

    // Detalles del conteo físico
    private BigDecimal bills200;
    private BigDecimal bills100;
    private BigDecimal bills50;
    private BigDecimal bills20;
    private BigDecimal bills10;
    private BigDecimal coins;
    private BigDecimal vouchers;

    // O contar total directamente
    private BigDecimal countedCash;

    private String notes;
}