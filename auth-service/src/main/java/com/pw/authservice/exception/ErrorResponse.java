package com.pw.authservice.exception;

public record ErrorResponse(boolean success, String message, Object data) {}
