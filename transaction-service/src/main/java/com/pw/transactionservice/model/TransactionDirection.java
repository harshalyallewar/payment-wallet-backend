package com.pw.transactionservice.model;

public enum TransactionDirection {
    PAYMENT,    // Customer -> Merchant
    REFUND,     // Merchant -> Customer
    TRANSFER    // Customer -> Customer (peer-to-peer)
}
