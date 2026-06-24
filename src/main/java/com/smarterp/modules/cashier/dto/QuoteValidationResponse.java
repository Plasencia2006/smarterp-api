package com.smarterp.modules.cashier.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuoteValidationResponse {
    private Boolean isValid;
    private String message;
    private List<String> warnings;
    private List<String> errors;
}