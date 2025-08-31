package com.pw.analyticsservice.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "auth_summary",
        uniqueConstraints = @UniqueConstraint(name="uk_auth_user_date", columnNames = {"user_id","date"}))
public class AuthSummary {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="date", nullable = false)
    private LocalDate date;

    @Column(name="user_id")
    private Long userId;

    @Column(name="logins", nullable = false)
    private Integer logins = 0;

    @Column(name="logouts", nullable = false)
    private Integer logouts = 0;

    @Column(name="failed_logins", nullable = false)
    private Integer failedLogins = 0;

    @Column(name="token_refreshes", nullable = false)
    private Integer tokenRefreshes = 0;

    @Column(name="last_updated", nullable = false)
    private LocalDateTime lastUpdated = LocalDateTime.now();

    // getters/setters...
}

