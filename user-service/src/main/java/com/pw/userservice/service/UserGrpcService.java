package com.pw.userservice.service;

import com.pw.userservice.dto.UserRequestDTO;
import com.pw.userservice.dto.UserResponseDTO;
import com.pw.userservice.model.User;
import com.pw.userservice.repository.UserRepository;
import com.userservice.grpc.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import jakarta.persistence.EntityExistsException;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;

import java.util.Optional;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserService userService;
    private final UserRepository userRepository;

    @Override
    public void createUser(CreateUserRequest request, StreamObserver<CreateUserResponse> responseObserver) {
        try {
            // Map request → DTO
            UserRequestDTO userRequestDTO = UserRequestDTO.builder()
                    .name(request.getName())
                    .email(request.getEmail())
                    .phoneNumber(request.getPhoneNumber())
                    .password(request.getPassword())
                    .type(request.getType())
                    .status(request.getStatus())
                    .build();

            log.info("Received CreateUserRequest: {}", userRequestDTO);

            // Call service
            UserResponseDTO userResponseDTO = userService.createUser(userRequestDTO);
            log.info("User created successfully: {}", userResponseDTO);

            // Build gRPC response
            CreateUserResponse createUserResponse = CreateUserResponse.newBuilder()
                    .setUserId(userResponseDTO.getId().toString())
                    .setSuccess(true)
                    .build();

            responseObserver.onNext(createUserResponse);
            responseObserver.onCompleted();

        } catch (ConstraintViolationException e) {
            log.warn("Validation failed: {}", e.getMessage(), e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Validation failed: " + e.getMessage())
                            .asRuntimeException()
            );

        } catch (EntityExistsException e) {
            log.warn("Entity already exists: {}", e.getMessage(), e);
            responseObserver.onError(
                    Status.ALREADY_EXISTS
                            .withDescription("User already exists")
                            .asRuntimeException()
            );

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity error while creating user: {}", e.getMessage(), e);
            responseObserver.onError(
                    Status.ALREADY_EXISTS
                            .withDescription("User already exists with given email/phone")
                            .asRuntimeException()
            );

        } catch (DataAccessException e) {
            log.error("Database error while creating user", e);
            responseObserver.onError(
                    Status.UNAVAILABLE
                            .withDescription("Database temporarily unavailable")
                            .asRuntimeException()
            );

        } catch (Exception e) {
            log.error("Unexpected error while creating user", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Unexpected error: " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void getUser(UserRequest request, StreamObserver<UserResponse> responseObserver) {
        try {
            log.info("Fetching user with email: {}", request.getEmail());

            Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

            if (userOptional.isEmpty()) {
                log.warn("User not found with email: {}", request.getEmail());
                responseObserver.onError(
                        Status.NOT_FOUND
                                .withDescription("User not found with email: " + request.getEmail())
                                .asRuntimeException()
                );
                return;
            }

            User user = userOptional.get();

            // Build response
            UserResponse userResponse = UserResponse.newBuilder()
                    .setUserId(user.getId().toString())
                    .setName(user.getName())
                    .setEmail(user.getEmail())
                    .setPhoneNumber(user.getPhoneNumber())
                    .setType(UserType.valueOf(user.getType().toString()))
                    .setStatus(user.getStatus())
                    .setPassword(user.getPassword()) // careful in prod: don’t expose hash
                    .setSuccess(true)
                    .build();

            responseObserver.onNext(userResponse);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            log.warn("Invalid input when fetching user: {}", request.getEmail(), e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Invalid input: " + e.getMessage())
                            .asRuntimeException()
            );

        } catch (DataAccessException e) {
            log.error("Database error while fetching user", e);
            responseObserver.onError(
                    Status.UNAVAILABLE
                            .withDescription("Database temporarily unavailable")
                            .asRuntimeException()
            );

        } catch (Exception e) {
            log.error("Unexpected error while fetching user: {}", request.getEmail(), e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Unexpected error: " + e.getMessage())
                            .asRuntimeException()
            );
        }
    }
}
