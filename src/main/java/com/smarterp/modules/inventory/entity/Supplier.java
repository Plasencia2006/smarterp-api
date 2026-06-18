package com.smarterp.modules.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // ✅ SOLO String, NO relación @ManyToOne Business
    @Column(name = "business_id", nullable = false)
    private String businessId;

    @Column(nullable = false)
    private String name;

    @Column
    private String ruc;

    @Column(name = "contact_name")
    private String contactName;

    @Column
    private String phone;

    @Column
    private String email;

    @Column
    private String address;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}