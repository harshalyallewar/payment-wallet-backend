package com.pw.authservice.dto;

import lombok.*;
import jakarta.validation.constraints.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegisterRequest {
    @NotBlank @Email
    private String email;

    @NotBlank
    private String password;

    // optional role; default ROLE_CUSTOMER
    private String role;
}
