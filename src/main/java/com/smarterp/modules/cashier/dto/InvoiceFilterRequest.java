package com.smarterp.modules.cashier.dto;

import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceFilterRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private String customerName;
    private String invoiceNumber;
    private String paymentMethod;
}