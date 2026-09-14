package com.agentcommerce.domain.identity;

class UserConflictException extends RuntimeException {

    UserConflictException(String message) {
        super(message);
    }
}
