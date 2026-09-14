package com.agentcommerce.domain.identity;

class UserResourceNotFoundException extends RuntimeException {

    UserResourceNotFoundException(String message) {
        super(message);
    }
}
