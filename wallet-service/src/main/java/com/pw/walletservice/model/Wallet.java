package com.pw.walletservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "wallets",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_wallet_user_type",
                        columnNames = {"user_id"}
                ),
                @UniqueConstraint(name = "uk_wallet_request", columnNames = {"request_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;   // Reference to User Service

    @Enumerated(EnumType.STRING)
    @Column(name = "wallet_type", nullable = false, length = 20)
    private WalletType walletType; // CUSTOMER or MERCHANT

    @Column(nullable = false, precision = 19, scale = 2)
    private Integer balance = 0; // Start with 0

    @Builder.Default
    @Column(nullable = false, length = 3)
    private String currency = "INR"; // Default currency

    @Column(nullable = false)  // prevent duplicates, do , unique = true later
    private String requestId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version; // Optimistic locking for concurrency-safe balance updates

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
