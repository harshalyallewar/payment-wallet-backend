package com.pw.analyticsservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_user_summary",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_date", columnNames = {"user_id","date"}))
public class DailyUserSummary {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable = false)
    private Long userId;

    @Column(name="date", nullable = false)
    private LocalDate date;

    @Column(name="total_credits", nullable = false)
    private BigDecimal totalCredits = BigDecimal.ZERO;

    @Column(name="total_debits", nullable = false)
    private BigDecimal totalDebits = BigDecimal.ZERO;

    @Column(name="failed_txns", nullable = false)
    private Integer failedTxns = 0;

    @Column(name="net_change", nullable = false)
    private BigDecimal netChange = BigDecimal.ZERO;

    @Column(name="last_updated", nullable = false)
    private LocalDateTime lastUpdated = LocalDateTime.now();

    // getters/setters...
}
