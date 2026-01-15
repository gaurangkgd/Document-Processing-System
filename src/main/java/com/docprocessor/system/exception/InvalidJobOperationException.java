package com.docprocessor.system.exception;

public class InvalidJobOperationException extends RuntimeException {
    public InvalidJobOperationException(String message) {
        super(message);
    }
}
