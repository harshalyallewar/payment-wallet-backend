package com.pw.authservice.exception;

public class GrpcServiceException extends RuntimeException {
    public GrpcServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

