package com.agentcommerce.domain.driver;

class DasherAccessDeniedException extends RuntimeException {

    DasherAccessDeniedException(String message) {
        super(message);
    }
}