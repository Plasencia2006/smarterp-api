package com.smarterp.modules.inventory.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkImportResult {
    private int total;
    private int success;
    private int failed;
    private List<String> errors;
}