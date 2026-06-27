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
public class AuditLogDTO {
    private String id;
    private String businessId;
    private String userId;
    private String userEmail;
    private String userName;
    private String action;
    private String entityType;
    private String entityId;
    private String oldValues;
    private String newValues;
    private String ipAddress;
    private String userAgent;
    private String status;
    private String details;
    private LocalDateTime createdAt;
}