package com.clinic.management.exception;

public class InvalidQueueOperationException extends RuntimeException {

    public InvalidQueueOperationException(String message) {
        super(message);
    }
}
