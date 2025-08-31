package com.pw.authservice.dto;

import com.userservice.grpc.AccountStatus;
import com.userservice.grpc.UserType;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequestDTO {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^[6-9]\\d{9}$", // Indian mobile validation (10 digits, starts with 6-9)
            message = "Phone number must be a valid 10-digit Indian mobile number"
    )
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    private String password;

    @NotNull(message = "User type is required (CUSTOMER, MERCHANT, ADMIN)")
    private UserType type;

    // Optional: status set by system (default ACTIVE), so not required from client
    private AccountStatus status = AccountStatus.ACTIVE;

    // Wallet balance should NOT be set by client during signup → default 0.0 in entity
}
