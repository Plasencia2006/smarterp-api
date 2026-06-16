package com.smarterp.modules.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "count_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CountItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "physical_count_id", nullable = false)
    private PhysicalCount physicalCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer countedQuantity;
    private Integer difference;
}