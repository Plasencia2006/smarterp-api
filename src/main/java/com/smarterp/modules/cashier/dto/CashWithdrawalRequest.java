package com.smarterp.modules.cashier.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashWithdrawalRequest {
    private String registerId;
    private BigDecimal amount;
    private String reason;
    private String destination; // "Caja Fuerte", "Banco", etc.
}