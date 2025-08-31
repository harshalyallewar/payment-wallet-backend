package com.pw.analyticsservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_system_summary", uniqueConstraints = @UniqueConstraint(columnNames = "date"))
public class DailySystemSummary {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="date", nullable = false)
    private LocalDate date;

    @Column(name="total_users", nullable = false)
    private Integer totalUsers = 0;

    @Column(name="new_users", nullable = false)
    private Integer newUsers = 0;

    @Column(name="total_txns", nullable = false)
    private Integer totalTxns = 0;

    @Column(name="failed_txns", nullable = false)
    private Integer failedTxns = 0;

    @Column(name="total_volume", nullable = false)
    private BigDecimal totalVolume = BigDecimal.ZERO;

    @Column(name="last_updated", nullable = false)
    private LocalDateTime lastUpdated = LocalDateTime.now();

    // getters/setters...
}

