package com.pw.transactionservice.model;

public enum TransactionStatus {
    SUCCESS,   // Transaction completed successfully
    FAILED,    // Transaction failed (e.g. insufficient balance, invalid wallet)
    PENDING    // Transaction is in progress / waiting for confirmation
}
