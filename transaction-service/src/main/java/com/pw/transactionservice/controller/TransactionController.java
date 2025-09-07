package com.pw.transactionservice.controller;

import com.pw.transactionservice.dto.CreditRequestDTO;
import com.pw.transactionservice.dto.DebitRequestDTO;
import com.pw.transactionservice.dto.TransferRequestDTO;
import com.pw.transactionservice.model.Transaction;
import com.pw.transactionservice.service.TransactionService;
import com.walletservice.grpc.WalletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Initiates a transfer (debit sender, credit receiver)
     */
    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(@Valid @RequestBody TransferRequestDTO request) {
        log.info("Transfer request: sender={}, receiver={}, amount={}", request.getSenderId(), request.getReceiverId(), request.getAmount());
        WalletResponse walletResponse = transactionService.transfer(request.getSenderId(), request.getReceiverId(), request.getAmount());
        return ResponseEntity.ok(walletResponse.toString());
    }

    /**
     * Debit a user’s wallet (e.g. when paying externally)
     */
    @PostMapping("/debit")
    public ResponseEntity<Transaction> debit(@Valid @RequestBody DebitRequestDTO request) {
        Transaction txn = transactionService.debit(
                request.getUserId(),
                request.getAmount(),
                request.getReferenceId()
        );
        return ResponseEntity.ok(txn);
    }

    /**
     * Credit a user’s wallet (e.g. refund, top-up)
     */
    @PostMapping("/credit")
    public ResponseEntity<Transaction> credit(@Valid @RequestBody CreditRequestDTO request) {
        Transaction txn = transactionService.credit(
                request.getUserId(),
                request.getAmount(),
                request.getReferenceId()
        );
        return ResponseEntity.ok(txn);
    }

    /**
     * Get all transactions of a user (history)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Transaction>> getUserTransactions(@PathVariable Long userId) {
        return ResponseEntity.ok(transactionService.getTransactionsByUser(userId));
    }

    /**
     * Get details of a single transaction
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<Transaction> getTransaction(@PathVariable Long transactionId) {
        return ResponseEntity.ok(transactionService.getTransactionById(transactionId));
    }

    /**
     * Get all transactions in the system (for admin/audit)
     */
    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }
}
