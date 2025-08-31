package com.pw.userservice.dto;

import com.userservice.grpc.AccountStatus;
import com.userservice.grpc.UserType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;      // Safe to return
    private UserType type;           // CUSTOMER / MERCHANT / ADMIN
    private AccountStatus status;    // ACTIVE / SUSPENDED / CLOSED
    private LocalDateTime createdAt; // Account creation timestamp
}
