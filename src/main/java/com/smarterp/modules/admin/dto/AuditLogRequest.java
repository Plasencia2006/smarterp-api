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
public class AuditLogRequest {
    private String action;
    private String entityType;
    private String entityId;
    private String oldValues;
    private String newValues;
    private String status;
    private String details;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String userId;
}