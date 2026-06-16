package com.smarterp.modules.support.entity;

import com.smarterp.shared.entity.Business;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "maintenances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Maintenance {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(nullable = false)
    private String description;
    @Column(name = "scheduled_date", nullable = false)
    private LocalDateTime scheduledDate;
    @Column(name = "completed_date")
    private LocalDateTime completedDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MaintenanceStatus status = MaintenanceStatus.SCHEDULED;

    @PrePersist
    protected void onCreate() {
        /* createdAt logic if needed */ }
}