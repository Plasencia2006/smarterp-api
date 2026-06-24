package com.smarterp.modules.cashier.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashRegisterOpenRequest {
    private BigDecimal initialCash;
    private String openingNotes;
}