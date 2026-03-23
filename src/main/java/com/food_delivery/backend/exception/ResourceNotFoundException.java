package com.food_delivery.backend.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BaseException {
    public ResourceNotFoundException(String message) {
        super(message, "NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
