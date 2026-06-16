package com.smarterp.modules.inventory.entity;

import com.smarterp.shared.entity.Business;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "physical_counts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhysicalCount {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PhysicalCountStatus status = PhysicalCountStatus.IN_PROGRESS;

    @OneToMany(mappedBy = "physicalCount", cascade = CascadeType.ALL)
    @Builder.Default
    private List<CountItem> items = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}