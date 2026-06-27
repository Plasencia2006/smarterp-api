package com.smarterp.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRequest {
    private String reportType; // SALES, INVENTORY, CASH, USERS, etc.
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String format; // PDF, EXCEL, CSV
    private String businessId;
    private String filterBy; // Additional filter
}