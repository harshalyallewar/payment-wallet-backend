package com.pw.authservice.controller;

import com.pw.authservice.dto.LoginRequest;
import com.pw.authservice.dto.RefreshRequest;
import com.pw.authservice.dto.TokenResponse;
import com.pw.authservice.dto.UserRequestDTO;
import com.pw.authservice.service.AuthService;
import com.userservice.grpc.CreateUserRequest;
import com.userservice.grpc.CreateUserResponse;
import com.userservice.grpc.UserRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Auth service is running");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRequestDTO request) {
        log.info("Registering user: {}", request.toString());
        CreateUserResponse response = authService.createUser(
                request
        );
        return ResponseEntity.ok("User Registered Successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        log.info("Login attempt for email: {}", request.toString());
        TokenResponse tokenResponse = authService.authenticate(request.getEmail(), request.getPassword());

        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        String response = authService.refreshAccessToken(refreshToken);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        authService.logout(userId);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

}
