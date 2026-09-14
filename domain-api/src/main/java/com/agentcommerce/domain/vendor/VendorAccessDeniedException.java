package com.agentcommerce.domain.vendor;

class VendorAccessDeniedException extends RuntimeException {

    VendorAccessDeniedException(String message) {
        super(message);
    }
}
