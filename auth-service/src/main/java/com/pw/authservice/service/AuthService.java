package com.pw.authservice.service;

import com.pw.authservice.dto.TokenResponse;
import com.pw.authservice.dto.UserRequestDTO;
import com.pw.authservice.entity.RefreshToken;
import com.pw.authservice.exception.*;
import com.pw.authservice.repository.RefreshTokenRepository;
import com.pw.authservice.security.JwtUtil;
import com.userservice.grpc.*;
import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class AuthService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    private final UserServiceGrpc.UserServiceBlockingStub userStub;

    public AuthService(
            RefreshTokenRepository refreshTokenRepository,
            JwtUtil jwtUtil,
            PasswordEncoder passwordEncoder,
            @GrpcClient("user-service") UserServiceGrpc.UserServiceBlockingStub userStub
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.userStub = userStub;
    }

    public TokenResponse authenticate(String email, String password) {
        try {
            log.info("Authenticating user with email: {}", email);

            UserRequest request = UserRequest.newBuilder()
                    .setEmail(email)
                    .build();

            UserResponse userResponse;
            try {
                userResponse = userStub.getUser(request);
            } catch (Exception e) {
                log.error("gRPC call to UserService failed", e);
                throw new GrpcServiceException("UserService unavailable", e);
            }

            if (!userResponse.getSuccess()) {
                throw new AuthenticationFailedException("User not found");
            }
            log.info("User found: {}", userResponse.toString());

            if (!passwordEncoder.matches(password, userResponse.getPassword())) {
                throw new AuthenticationFailedException("Invalid credentials");
            }



            String userId = userResponse.getUserId();
            String role = String.valueOf(userResponse.getType());

            String accessToken = jwtUtil.generateAccessToken(userId, email, role);
            String refreshToken = jwtUtil.generateRefreshToken(userId, email);

            // Store refresh token
            try {
                RefreshToken rt = new RefreshToken();
                rt.setUserId(userId);
                rt.setToken(refreshToken);
                rt.setExpiresAt(Instant.now().plusMillis(604800000)); // 7 days
                refreshTokenRepository.save(rt);
            } catch (DataAccessException dae) {
                log.error("Failed to store refresh token", dae);
                throw new TokenRefreshException("Could not store refresh token", dae);
            }

            return new TokenResponse(accessToken, refreshToken);

        } catch (AuthenticationFailedException | TokenRefreshException | GrpcServiceException ex) {
            throw ex; // propagate to global handler
        } catch (Exception ex) {
            log.error("Unexpected error during authentication", ex);
            throw new AuthenticationFailedException("Authentication failed due to unexpected error", ex);
        }
    }

    public String refreshAccessToken(String refreshToken) {
        try {
            RefreshToken rt = refreshTokenRepository.findByToken(refreshToken)
                    .orElseThrow(() -> new TokenRefreshException("Refresh token expired or invalid"));

            if (rt.getExpiresAt().isBefore(Instant.now())) {
                throw new TokenRefreshException("Refresh token expired or invalid");
            }

            Claims claims = jwtUtil.validateToken(rt.getToken());
            String userId = claims.get("userId", String.class);
            String email = claims.getSubject();
            String role = claims.get("role", String.class);

            return jwtUtil.generateAccessToken(userId, email, role);
        } catch (TokenRefreshException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to refresh access token", ex);
            throw new TokenRefreshException("Failed to refresh access token", ex);
        }
    }

    public CreateUserResponse createUser(UserRequestDTO userRequestDTO) {
        try {
            String hashedPassword = passwordEncoder.encode(userRequestDTO.getPassword());

            CreateUserRequest request = CreateUserRequest.newBuilder()
                    .setName(userRequestDTO.getName())
                    .setEmail(userRequestDTO.getEmail())
                    .setPassword(hashedPassword)
                    .setPhoneNumber(userRequestDTO.getPhoneNumber())
                    .setType(userRequestDTO.getType())
                    .setStatus(AccountStatus.ACTIVE)
                    .build();

            log.info("Creating user : {}", userRequestDTO.toString());

            CreateUserResponse response;
            try {
                response = userStub.createUser(request);
            } catch (Exception e) {
                log.error("gRPC call to UserService failed", e);
                throw new GrpcServiceException("UserService unavailable", e);
            }

            if (!response.getSuccess()) {
                throw new UserCreationException("Failed to create user");
            }

            return response;

        } catch (UserCreationException | GrpcServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error during user creation", ex);
            throw new UserCreationException("User creation failed due to unexpected error", ex);
        }
    }

    @Transactional
    public void logout(String userId) {
        try {
            refreshTokenRepository.deleteByUserId(userId);
        } catch (DataAccessException dae) {
            log.error("Failed to delete refresh token for user {}", userId, dae);
            throw new TokenRefreshException("Logout failed", dae);
        } catch (Exception ex) {
            log.error("Unexpected error during logout", ex);
            throw new TokenRefreshException("Logout failed due to unexpected error", ex);
        }
    }
}
