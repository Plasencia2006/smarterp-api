package com.smarterp.modules.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_business", columnList = "business_id"),
        @Index(name = "idx_audit_user", columnList = "user_id"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_timestamp", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "business_id", nullable = false)
    private String businessId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "user_name")
    private String userName;

    @Column(nullable = false)
    private String action; // CREATE, UPDATE, DELETE, LOGIN, LOGOUT, etc.

    @Column(name = "entity_type")
    private String entityType; // User, Product, Quote, etc.

    @Column(name = "entity_id")
    private String entityId;

    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues; // JSON

    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues; // JSON

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "status")
    private String status; // SUCCESS, FAILURE

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}