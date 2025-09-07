package com.pw.walletservice.service;

import com.pw.walletservice.kafka.KafkaEventProducer;
import com.pw.walletservice.model.EventEnvelope;
import com.walletservice.grpc.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import com.pw.walletservice.model.Wallet;
import com.pw.walletservice.model.WalletType;
import com.pw.walletservice.repository.WalletRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.TransactionSystemException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class WalletGrpcService extends WalletServiceGrpc.WalletServiceImplBase {

    private final WalletRepository walletRepository;
    private final KafkaEventProducer kafkaEventProducer;

    @Override
    public void getTransactions(TransactionHistoryRequest request, StreamObserver<TransactionHistoryResponse> responseObserver) {
//        try {
//            // not implemented yet
//            responseObserver.onNext(TransactionHistoryResponse.newBuilder()
//                    .setSuccess(false)
//                    .setMessage("Not implemented")
//                    .build());
//            responseObserver.onCompleted();
//        } catch (Exception e) {
//            handleError("Error fetching transactions", e, responseObserver);
//        }
    }

    @Override
    public void createWallet(CreateWalletRequest request, StreamObserver<WalletResponse> responseObserver) {
        try {
            Wallet wallet = Wallet.builder()
                    .userId(request.getUserId())
                    .walletType(WalletType.valueOf(request.getWalletType().name()))
                    .balance(0)
                    .requestId(request.getRequestId())
                    .build();

            log.info("Creating wallet = {}", wallet);
            Wallet saved = walletRepository.save(wallet);

            WalletResponse response = WalletResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Wallet created successfully")
                    .setBalance(0)
                    .setRequestId(UUID.randomUUID().toString())
                    .setUpdatedAt(Instant.now().toString())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Wallet created successfully :{}", saved.toString());

        } catch (IllegalArgumentException e) {
            handleError("Invalid wallet type: " + e.getMessage(), e, responseObserver, Status.INVALID_ARGUMENT);
        } catch (DataIntegrityViolationException e) {
            handleError("Wallet data violates database constraints (duplicate userId, FK, or not-null): " + e.getMessage(),
                    e, responseObserver, Status.ALREADY_EXISTS);
        } catch (EntityExistsException e) {
            handleError("Wallet already exists for given user: " + e.getMessage(),
                    e, responseObserver, Status.ALREADY_EXISTS);
        } catch (TransactionSystemException e) {
            handleError("Transaction failed while creating wallet (possibly validation or DB issue): " + e.getMessage(),
                    e, responseObserver, Status.ABORTED);
        } catch (Exception e) {
            handleError("Unexpected error while creating wallet: " + e.getMessage(),
                    e, responseObserver, Status.INTERNAL);
        }
    }

    @Override
    public void getWallet(GetWalletRequest request, StreamObserver<WalletResponse> responseObserver) {
        try {
            Wallet wallet = walletRepository.findByUserId(request.getUserId())
                    .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                            "Wallet not found for userId=" + request.getUserId()
                    ));

            WalletResponse response = WalletResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Wallet retrieved successfully")
                    .setBalance(wallet.getBalance())
                    .setRequestId(wallet.getRequestId())
                    .setUpdatedAt(Instant.now().toString())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            // Validation issue → BAD REQUEST
            handleError("Invalid argument when fetching wallet", e, responseObserver, Status.INVALID_ARGUMENT);

        } catch (EntityNotFoundException | NoResultException e) {
            // JPA "not found" → NOT_FOUND
            handleError(e.getMessage(), e, responseObserver, Status.NOT_FOUND);

        } catch (DataAccessException e) {
            // Database access problems (connection, transaction, constraint, etc.) → UNAVAILABLE
            handleError("Database error while fetching wallet", e, responseObserver, Status.UNAVAILABLE);

        } catch (Exception e) {
            // Catch-all fallback
            handleError("Unexpected error while fetching wallet", e, responseObserver, Status.INTERNAL);
        }
    }


    @Override
    public void credit(WalletOperationRequest request, StreamObserver<WalletResponse> responseObserver) {
        try {
            // Validate amount
            if (request.getAmount() <= 0) {
                throw new IllegalArgumentException("Credit amount must be greater than zero");
            }

            // Fetch wallet
            Wallet wallet = walletRepository.findByUserId(request.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("Wallet not found for userId=" + request.getUserId()));

            // Update balance
            wallet.setBalance(wallet.getBalance() + request.getAmount());
            Wallet saved = walletRepository.save(wallet);

            WalletResponse response = WalletResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Amount credited successfully")
                    .setBalance(wallet.getBalance())
                    .setRequestId(request.getRequestId())
                    .setUpdatedAt(Instant.now().toString())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Wallet credited successfully :{}", wallet.toString());

            Map<String, Object> map = new HashMap<>();
            map.put("amount", String.valueOf(request.getAmount()));

            EventEnvelope event = new EventEnvelope();
            event.setEventId(UUID.randomUUID().toString());
            event.setEventType("WALLET_CREDITED");
            event.setTimestamp(Instant.now());
            event.setUserId(saved.getId());
            event.setPayload(map);

            kafkaEventProducer.sendEvent("wallet-events", event);
            log.info("Published WALLET_CREDITED event to Kafka for userId={}", saved.getUserId());

        } catch (IllegalArgumentException e) {
            handleError("Invalid input: " + e.getMessage(), e, responseObserver, Status.INVALID_ARGUMENT, request.getUserId(), request.getAmount());
        } catch (EntityNotFoundException e) {
            handleError("Wallet not found: " + e.getMessage(), e, responseObserver, Status.NOT_FOUND, request.getUserId(), request.getAmount());
        } catch (OptimisticLockingFailureException e) {
            handleError("Concurrent modification detected", e, responseObserver, Status.ABORTED, request.getUserId(), request.getAmount());
        } catch (DataIntegrityViolationException e) {
            handleError("Data integrity violation: " + e.getMessage(), e, responseObserver, Status.FAILED_PRECONDITION, request.getUserId(), request.getAmount());
        } catch (Exception e) {
            handleError("Unexpected error while crediting wallet", e, responseObserver, Status.INTERNAL, request.getUserId(), request.getAmount());
        }
    }


    @Override
    public void debit(WalletOperationRequest request, StreamObserver<WalletResponse> responseObserver) {
        try {
            // Validate amount
            if (request.getAmount() <= 0) {
                throw new IllegalArgumentException("Debit amount must be greater than zero");
            }

            // Fetch wallet
            Wallet wallet = walletRepository.findByUserId(request.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("Wallet not found for userId=" + request.getUserId()));

            // Check balance
            if (wallet.getBalance() < request.getAmount()) {
                WalletResponse failResponse = WalletResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Insufficient balance")
                        .setBalance(wallet.getBalance())
                        .setRequestId(request.getRequestId())
                        .setUpdatedAt(Instant.now().toString())
                        .build();

                responseObserver.onNext(failResponse);
                responseObserver.onCompleted();
                return;
            }

            // Deduct balance
            wallet.setBalance(wallet.getBalance() - request.getAmount());
            Wallet saved = walletRepository.save(wallet);

            WalletResponse response = WalletResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Amount debited successfully")
                    .setBalance(wallet.getBalance())
                    .setRequestId(request.getRequestId())
                    .setUpdatedAt(Instant.now().toString())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Wallet debited successfully :{}", wallet.toString());

            Map<String, Object> map = new HashMap<>();
            map.put("amount", String.valueOf(request.getAmount()));

            EventEnvelope event = new EventEnvelope();
            event.setEventId(UUID.randomUUID().toString());
            event.setEventType("WALLET_DEBITED");
            event.setTimestamp(Instant.now());
            event.setUserId(saved.getId());
            event.setPayload(map);

            kafkaEventProducer.sendEvent("wallet-events", event);
            log.info("Published WALLET_CREDITED event to Kafka for userId={}", saved.getUserId());

        } catch (IllegalArgumentException e) {
            handleError("Invalid input: " + e.getMessage(), e, responseObserver, Status.INVALID_ARGUMENT, request.getUserId(), request.getAmount());
        } catch (EntityNotFoundException e) {
            handleError("Wallet not found: " + e.getMessage(), e, responseObserver, Status.NOT_FOUND, request.getUserId(), request.getAmount());
        } catch (OptimisticLockingFailureException e) {
            handleError("Concurrent modification detected", e, responseObserver, Status.ABORTED, request.getUserId(), request.getAmount());
        } catch (DataIntegrityViolationException e) {
            handleError("Data integrity violation: " + e.getMessage(), e, responseObserver, Status.FAILED_PRECONDITION, request.getUserId(), request.getAmount());
        } catch (Exception e) {
            handleError("Unexpected error while debiting wallet", e, responseObserver, Status.INTERNAL, request.getUserId(), request.getAmount());
        }
    }

    @Override
    @Transactional
    public void transfer(TransferRequest request, StreamObserver<WalletResponse> responseObserver) {
        try {
            // Validate amount
            if (request.getAmount() <= 0) {
                throw new IllegalArgumentException("Transfer amount must be greater than zero");
            }

            // Duplicate request check
            Optional<Wallet> existing = walletRepository.findByRequestId(request.getRequestId());
            if (existing.isPresent()) {
                log.info("Duplicate request detected for requestId={}", request.getRequestId());
                Wallet wallet = existing.get();

                WalletResponse duplicateResponse = WalletResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Duplicate transfer request")
                        .setBalance(wallet.getBalance())
                        .setRequestId(wallet.getRequestId())
                        .setUpdatedAt(wallet.getUpdatedAt().toString())
                        .build();

                responseObserver.onNext(duplicateResponse);
                responseObserver.onCompleted();
                return;
            }

            // Fetch wallets
            Wallet fromWallet = walletRepository.findByUserId(request.getFromUserId())
                    .orElseThrow(() -> new EntityNotFoundException("Source wallet not found for userId=" + request.getFromUserId()));

            Wallet toWallet = walletRepository.findByUserId(request.getToUserId())
                    .orElseThrow(() -> new EntityNotFoundException("Target wallet not found for userId=" + request.getToUserId()));

            log.info("Transferring {} from user {} to user {}", request.getAmount(), fromWallet.getUserId(), toWallet.getUserId());
            // Check balance
            if (fromWallet.getBalance() < request.getAmount()) {
                WalletResponse insufficientResponse = WalletResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Insufficient balance for transfer")
                        .setBalance(fromWallet.getBalance())
                        .setRequestId(request.getRequestId())
                        .setUpdatedAt(Instant.now().toString())
                        .build();

                responseObserver.onNext(insufficientResponse);
                responseObserver.onCompleted();
                return;
            }

            // Perform transfer
            fromWallet.setBalance(fromWallet.getBalance() - request.getAmount());
            toWallet.setBalance(toWallet.getBalance() + request.getAmount());

            // Persist with same requestId for idempotency
            fromWallet.setRequestId(request.getRequestId());
            fromWallet.setUpdatedAt(LocalDateTime.now());
            toWallet.setUpdatedAt(LocalDateTime.now());

            walletRepository.save(fromWallet);
            walletRepository.save(toWallet);

            log.info("Transfer complete: {} from user {} to user {}", request.getAmount(), fromWallet.getUserId(), toWallet.getUserId());

            // Success response
            WalletResponse response = WalletResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Transfer successful")
                    .setBalance(fromWallet.getBalance()) // Returning source wallet balance
                    .setRequestId(request.getRequestId())
                    .setUpdatedAt(Instant.now().toString())
                    .build();

            log.info("Transfer response: {}", response);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Wallet transfer successfully from :{} to: {}", fromWallet.toString(), toWallet.toString());

            // payload: { fromUserId, toUserId, amount, success }
            Map<String, Object> map = new HashMap<>();
            map.put("amount", String.valueOf(request.getAmount()));
            map.put("fromUserId", String.valueOf(fromWallet.getUserId()));
            map.put("toUserId", String.valueOf(toWallet.getUserId()));
            map.put("success", Boolean.TRUE);

            EventEnvelope event = new EventEnvelope();
            event.setEventId(UUID.randomUUID().toString());
            event.setEventType("WALLET_FAILED");
            event.setTimestamp(Instant.now());
            event.setUserId(fromWallet.getUserId());
            event.setPayload(map);

            kafkaEventProducer.sendEvent("wallet-events", event);
            log.info("Published transfer event to Kafka from user wallet ={} toUser wallet={}", fromWallet.getUserId(), toWallet.getUserId());

        } catch (IllegalArgumentException e) {
            handleError("Invalid transfer request: " + e.getMessage(), e, responseObserver, Status.INVALID_ARGUMENT, request.getFromUserId(), request.getAmount());
        } catch (EntityNotFoundException e) {
            handleError("Wallet not found: " + e.getMessage(), e, responseObserver, Status.NOT_FOUND, request.getFromUserId(), request.getAmount());
        } catch (OptimisticLockingFailureException e) {
            handleError("Concurrent transfer conflict", e, responseObserver, Status.ABORTED, request.getFromUserId(), request.getAmount());
        } catch (DataIntegrityViolationException e) {
            handleError("Data integrity violation during transfer", e, responseObserver, Status.FAILED_PRECONDITION, request.getFromUserId(), request.getAmount());
        } catch (Exception e) {
            handleError("Unexpected error during transfer", e, responseObserver, Status.INTERNAL, request.getFromUserId(), request.getAmount());
        }
    }

    // ---------- Common Exception Handler ----------
    private <T> void handleError(String message, Exception e, StreamObserver<T> responseObserver) {
        handleError(message, e, responseObserver, Status.INTERNAL);
    }

    private <T> void handleError(String message, Exception e, StreamObserver<T> responseObserver, Status status) {
        log.error("{}: {}", message, e.getMessage(), e);
        responseObserver.onError(status.withDescription(message).withCause(e).asRuntimeException());
    }

    // Overloaded method with userId and amount details
    private <T> void handleError(String message, Exception e, StreamObserver<T> responseObserver, Status status,
                                 Long userId, int amount) {
        log.error("{}: {}", message, e.getMessage(), e);
        responseObserver.onError(status.withDescription(message).withCause(e).asRuntimeException());

        // Create the payload with additional details
        Map<String, Object> map = new HashMap<>();
        map.put("amount", String.valueOf(amount));
        map.put("userId", String.valueOf(userId));
        if (userId != null) {
            map.put("userId", String.valueOf(userId));
        }
        map.put("success", Boolean.FALSE);

        EventEnvelope event = new EventEnvelope();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("WALLET_FAILED");
        event.setTimestamp(Instant.now());
        event.setUserId(userId);
        event.setPayload(map);

        kafkaEventProducer.sendEvent("wallet-events", event);
        log.info("Published WALLET_FAILED event to Kafka from user wallet={} to user wallet={} failed", userId, userId);
    }

}
