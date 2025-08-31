package com.pw.userservice.grpc;

import com.pw.userservice.model.User;
import com.walletservice.grpc.CreateWalletRequest;
import com.walletservice.grpc.WalletResponse;
import com.walletservice.grpc.WalletServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletGrpcService {

    private final WalletServiceGrpc.WalletServiceBlockingStub walletStub;

    public void createWallet(User user) {
        try {
            // 1. Save user to DB
            // (assumed done before calling this method)

            // 2. Call wallet service via gRPC
            CreateWalletRequest walletRequest = CreateWalletRequest.newBuilder()
                    .setUserId(user.getId())
                    .setWalletType(user.getWalletType())
                    .setRequestId(UUID.randomUUID().toString())
                    .build();

            WalletResponse response = walletStub.createWallet(walletRequest);

            if (!response.getSuccess()) {
                // Wallet service responded but with a failure
                log.warn("Wallet creation failed for user={} : {}", user.getEmail(), response.getMessage());
                throw new RuntimeException("Wallet creation failed: " + response.getMessage());
            }

            log.info("Created wallet via gRPC for user={} walletType={} balance={}",
                    user.getEmail(), user.getWalletType(), response.getBalance());

        } catch (StatusRuntimeException e) {
            // Handle different gRPC status codes
            Status.Code code = e.getStatus().getCode();
            switch (code) {
                case UNAVAILABLE -> log.error("Wallet service unavailable when creating wallet for user={}", user.getEmail(), e);
                case DEADLINE_EXCEEDED -> log.error("Timeout while creating wallet for user={}", user.getEmail(), e);
                case ALREADY_EXISTS -> log.warn("Wallet already exists for user={}", user.getEmail(), e);
                default -> log.error("Unexpected gRPC error while creating wallet for user={}", user.getEmail(), e);
            }
            throw new RuntimeException("Failed to create wallet due to gRPC error: " + code, e);

        } catch (Exception e) {
            log.error("Unexpected error while creating wallet for user={}", user.getEmail(), e);
            throw new RuntimeException("Failed to create wallet", e);
        }
    }
}
