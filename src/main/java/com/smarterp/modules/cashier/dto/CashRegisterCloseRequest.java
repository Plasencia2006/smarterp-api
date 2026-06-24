package com.smarterp.modules.cashier.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashRegisterCloseRequest {
    private BigDecimal finalCash;
    private String closingNotes;
}