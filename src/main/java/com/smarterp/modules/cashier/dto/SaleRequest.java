package com.smarterp.modules.cashier.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class SaleRequest {
    private String customerId;
    @NotEmpty(message = "La venta debe tener al menos un item")
    @Valid
    private List<SaleItemRequest> items;
    @Valid
    private List<PaymentRequest> payments;
}