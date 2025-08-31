package com.pw.userservice.dto;

import com.userservice.grpc.UserType;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequestDTO {

    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;

    @Email(message = "Email format is invalid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Pattern(
            regexp = "^[6-9]\\d{9}$", // Indian mobile validation (10 digits, starts with 6-9)
            message = "Phone number must be a valid 10-digit Indian mobile number"
    )
    private String phoneNumber;

    private UserType type;
}
