package com.pw.transactionservice.exception;


public record ErrorResponse(boolean success, String message, Object data) {}
