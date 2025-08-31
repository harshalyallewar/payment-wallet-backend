package com.pw.userservice.model;

import com.userservice.grpc.AccountStatus;
import com.userservice.grpc.UserType;
import com.walletservice.grpc.WalletType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_user_phone", columnNames = "phone_number")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Basic Information
    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, unique = true, length = 15)
    private String phoneNumber;

    // Security
    @Column(nullable = false)
    private String password;  // store only encrypted/hashed password (e.g. BCrypt)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserType type; // CUSTOMER / MERCHANT / ADMIN

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    public WalletType getWalletType(){
        return switch (this.type) {
            case MERCHANT -> WalletType.MERCHANT;
            default -> WalletType.CUSTOMER;
        };
    }

    // Lifecycle callbacks to auto-update timestamps
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
