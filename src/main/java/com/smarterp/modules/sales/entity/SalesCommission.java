package com.smarterp.modules.sales.entity;

import com.smarterp.shared.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "sales_commissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesCommission {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false)
    private BigDecimal amount;
    @Column(nullable = false)
    private BigDecimal percentage;
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;
}