package com.pw.userservice.service;

import com.pw.userservice.dto.UserRequestDTO;
import com.pw.userservice.dto.UserResponseDTO;
import com.pw.userservice.dto.UserUpdateRequestDTO;
import com.pw.userservice.exception.UserNotFoundException;
import com.pw.userservice.grpc.WalletGrpcService;
import com.pw.userservice.kafka.KafkaEventProducer;
import com.pw.userservice.model.EventEnvelope;
import com.pw.userservice.model.User;
import com.pw.userservice.repository.UserRepository;
import com.userservice.grpc.AccountStatus;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final WalletGrpcService walletGrpcService;
    private final KafkaEventProducer kafkaEventProducer;
    /**
     * Creates a new user and triggers wallet creation via gRPC.
     * Rolls back automatically on runtime exceptions.
     */
    @Transactional
    public UserResponseDTO createUser(UserRequestDTO request) {
        try {
            log.info("Creating user with email: {}", request.getEmail());

            // Check for duplicate email
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new EntityExistsException("User with email " + request.getEmail() + " already exists");
            }

            // Build new user
            User user = User.builder()
                    .name(request.getName().trim())
                    .email(request.getEmail().trim().toLowerCase())
                    .password(request.getPassword()) // ⚠️ hash in real-world
                    .phoneNumber(request.getPhoneNumber())
                    .type(request.getType())
                    .status(AccountStatus.PENDING_VERIFICATION)
                    .build();

            User saved = userRepository.save(user);
            log.info("User created successfully with id: {}", saved.getId());
            EventEnvelope event = new EventEnvelope();
            event.setEventId(UUID.randomUUID().toString());
            event.setEventType("USER_CREATED");
            event.setTimestamp(Instant.now());
            event.setUserId(saved.getId());
            event.setPayload(Collections.emptyMap());

            kafkaEventProducer.sendEvent("user-events", event);

            // gRPC call to wallet service
            try {
                walletGrpcService.createWallet(saved);
                log.info("Wallet created via gRPC for user {}", saved.getEmail());
            } catch (RuntimeException grpcEx) {
                // gRPC failure should not leave orphaned user silently
                log.error("Failed to create wallet for user {}. Rolling back user creation. Error={}",
                        saved.getEmail(), grpcEx.getMessage(), grpcEx);
                // force rollback
                throw grpcEx;
            }

            return mapToResponse(saved);

        } catch (EntityExistsException e) {
            log.warn("Duplicate email attempted: {}", request.getEmail());
            throw e;
        } catch (DataAccessException | PersistenceException e) {
            log.error("Database error while creating user with email {}: {}", request.getEmail(), e.getMessage(), e);
            throw e; // Spring will rollback automatically
        } catch (RuntimeException e) {
            log.error("Unexpected error while creating user: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Fetch all users.
     */
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        try {
            log.info("Fetching all users");
            return userRepository.findAll()
                    .stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            log.error("Database error while fetching all users: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Fetch user by ID.
     */
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long id) {
        try {
            log.info("Fetching user with id: {}", id);
            return userRepository.findById(id)
                    .map(this::mapToResponse)
                    .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        } catch (DataAccessException e) {
            log.error("Database error while fetching user {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Update user partially.
     */
    @Transactional
    public UserResponseDTO updateUser(Long userId, UserUpdateRequestDTO request) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

            if (request.getName() != null) {
                user.setName(request.getName().trim());
            }

            log.info("Updating user :", user.toString());

            if (request.getEmail() != null) {
                String newEmail = request.getEmail().trim().toLowerCase();
                // Only check if the email is actually changing
                if (!newEmail.equalsIgnoreCase(user.getEmail())) {
                    userRepository.findByEmail(newEmail)
                            .ifPresent(existing -> {
                                throw new EntityExistsException("Email already in use: " + newEmail);
                            });
                    user.setEmail(newEmail);
                }
            }

            if (request.getPhoneNumber() != null) {
                user.setPhoneNumber(request.getPhoneNumber());
            }

            if (request.getType() != null) {
                user.setType(request.getType());
            }

            User saved = userRepository.save(user);
            return mapToResponse(saved);

        } catch (UserNotFoundException | EntityExistsException e) {
            log.warn("Validation error while updating user {}: {}", userId, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while updating user {}: {}", userId, e.getMessage(), e);
            throw e;
        } catch (RuntimeException e) {
            log.error("Unexpected error while updating user {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    // helper
    private UserResponseDTO mapToResponse(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .type(user.getType())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
