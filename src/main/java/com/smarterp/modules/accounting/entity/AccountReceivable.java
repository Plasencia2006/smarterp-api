package com.smarterp.modules.accounting.entity;

import com.smarterp.shared.entity.Business;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "accounts_receivable")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountReceivable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(name = "customer_name", nullable = false)
    private String customerName;
    @Column(nullable = false)
    private BigDecimal amount;
    @Column(name = "paid_amount")
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AccountStatus status = AccountStatus.PENDING;
}