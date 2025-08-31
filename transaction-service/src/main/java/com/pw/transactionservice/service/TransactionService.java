package com.pw.transactionservice.service;

import com.pw.transactionservice.exception.InsufficientBalanceException;
import com.pw.transactionservice.exception.TransactionFailedException;
import com.pw.transactionservice.exception.TransactionNotFoundException;
import com.pw.transactionservice.exception.WalletServiceException;
import com.pw.transactionservice.model.Transaction;
import com.pw.transactionservice.model.TransactionStatus;
import com.pw.transactionservice.model.TransactionType;
import com.pw.transactionservice.repository.TransactionRepository;
import com.walletservice.grpc.TransferRequest;
import com.walletservice.grpc.WalletOperationRequest;
import com.walletservice.grpc.WalletResponse;
import com.walletservice.grpc.WalletServiceGrpc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletServiceGrpc.WalletServiceBlockingStub walletStub;

    public TransactionService(TransactionRepository transactionRepository,
                              WalletServiceGrpc.WalletServiceBlockingStub walletStub) {
        this.transactionRepository = transactionRepository;
        this.walletStub = walletStub;
    }

    /**
     * Transfer money between users (atomic)
     */
    @Transactional
    public WalletResponse transfer(Long senderId, Long receiverId, Integer amount) {
        WalletResponse response;
        String transferId = UUID.randomUUID().toString();
        String requestId = UUID.randomUUID().toString();

        TransactionStatus status;

        try {
            TransferRequest request = TransferRequest.newBuilder()
                    .setFromUserId(senderId)
                    .setToUserId(receiverId)
                    .setAmount(amount)
                    .setRequestId(requestId)
                    .build();

            log.info("Calling WalletService gRPC transfer: sender={}, receiver={}, amount={}", senderId, receiverId, amount);
            response = walletStub.transfer(request);
            if (!response.getSuccess()) {
                throw new InsufficientBalanceException("WalletService failed transfer due to insufficient balance");
            }
            status = TransactionStatus.SUCCESS;

        } catch (InsufficientBalanceException e) {
            log.error("Transfer failed due to insufficient balance: {}", e.getMessage());
            throw new InsufficientBalanceException("Sender has insufficient balance for the transfer");
        } catch (Exception e) {
            log.error("gRPC transfer failed: {}", e.getMessage(), e);
            throw new WalletServiceException("WalletService unavailable or transfer failed", e);
        }

        try {
            Transaction debit = Transaction.builder()
                    .userId(senderId)
                    .amount(amount)
                    .type(TransactionType.DEBIT)
                    .status(status)
                    .transferId(transferId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            transactionRepository.save(debit);

            Transaction credit = Transaction.builder()
                    .userId(receiverId)
                    .amount(amount)
                    .type(TransactionType.CREDIT)
                    .status(status)
                    .transferId(transferId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            transactionRepository.save(credit);

            return response;

        } catch (DataAccessException dae) {
            log.error("Database error while saving transfer transaction: {}", dae.getMessage(), dae);
            throw new TransactionFailedException("Failed to save transfer transaction");
        }
    }

    /**
     * Debit wallet
     */
    public Transaction debit(Long userId, Integer amount, String referenceId) {
        String transferId = UUID.randomUUID().toString();
        TransactionStatus status;

        try {
            WalletOperationRequest request = WalletOperationRequest.newBuilder()
                    .setUserId(userId)
                    .setAmount(amount)
                    .build();

            log.info("Calling WalletService gRPC debit: user={}, amount={}", userId, amount);
            WalletResponse response = walletStub.debit(request);
            if (!response.getSuccess()) {
                throw new WalletServiceException("WalletService failed debit operation", null);
            }
            status = TransactionStatus.SUCCESS;

        } catch (Exception e) {
            log.error("gRPC debit failed: {}", e.getMessage(), e);
            throw new WalletServiceException("WalletService unavailable or debit failed", e);
        }

        try {
            Transaction txn = Transaction.builder()
                    .userId(userId)
                    .amount(amount)
                    .type(TransactionType.DEBIT)
                    .status(status)
                    .transferId(transferId)
                    .referenceId(referenceId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            return transactionRepository.save(txn);

        } catch (DataAccessException dae) {
            log.error("Database error while saving debit transaction: {}", dae.getMessage(), dae);
            throw new TransactionFailedException("Failed to save debit transaction");
        }
    }

    /**
     * Credit wallet
     */
    public Transaction credit(Long userId, Integer amount, String referenceId) {
        String transferId = UUID.randomUUID().toString();
        TransactionStatus status;

        try {
            WalletOperationRequest request = WalletOperationRequest.newBuilder()
                    .setUserId(userId)
                    .setAmount(amount)
                    .build();

            log.info("Calling WalletService gRPC credit: user={}, amount={}", userId, amount);
            WalletResponse response = walletStub.credit(request);
            if (!response.getSuccess()) {
                throw new WalletServiceException("WalletService failed credit operation", null);
            }
            status = TransactionStatus.SUCCESS;

        } catch (Exception e) {
            log.error("gRPC credit failed: {}", e.getMessage(), e);
            throw new WalletServiceException("WalletService unavailable or credit failed", e);
        }

        try {
            Transaction txn = Transaction.builder()
                    .userId(userId)
                    .amount(amount)
                    .type(TransactionType.CREDIT)
                    .status(status)
                    .transferId(transferId)
                    .referenceId(referenceId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            return transactionRepository.save(txn);

        } catch (DataAccessException dae) {
            log.error("Database error while saving credit transaction: {}", dae.getMessage(), dae);
            throw new TransactionFailedException("Failed to save credit transaction");
        }
    }

    /**
     * Get transactions by user
     */
    public List<Transaction> getTransactionsByUser(Long userId) {
        try {
            return transactionRepository.findByUserId(userId);
        } catch (DataAccessException dae) {
            log.error("Failed to fetch transactions for user {}: {}", userId, dae.getMessage(), dae);
            throw new TransactionFailedException("Failed to fetch user transactions");
        }
    }

    /**
     * Get single transaction
     */
    public Transaction getTransactionById(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
    }

    /**
     * Get all transactions
     */
    public List<Transaction> getAllTransactions() {
        try {
            return transactionRepository.findAll();
        } catch (DataAccessException dae) {
            log.error("Failed to fetch all transactions: {}", dae.getMessage(), dae);
            throw new TransactionFailedException("Failed to fetch all transactions");
        }
    }
}
