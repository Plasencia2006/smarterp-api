package com.smarterp.modules.cashier.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoidInvoiceRequest {
    private String reason; // Motivo de la anulación
    private Boolean restoreStock; // Si debe restaurar el stock (default: true)
}