package com.smarterp.modules.cashier.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SaleItemRequest {
    @NotBlank
    private String productId;
    @NotNull
    @Min(1)
    private Integer quantity;
}