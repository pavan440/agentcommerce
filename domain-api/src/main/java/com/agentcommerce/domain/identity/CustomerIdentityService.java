package com.agentcommerce.domain.identity;

import java.util.Optional;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class CustomerIdentityService {

    private final CustomerIdentityRepository repository;

    CustomerIdentityService(CustomerIdentityRepository repository) {
        this.repository = repository;
    }

    public CustomerProfileResponse findOrProvision(Jwt jwt) {
        String issuer = Optional.ofNullable(jwt.getIssuer())
            .map(Object::toString)
            .orElseThrow(() -> new IllegalArgumentException("JWT issuer is required"));
        String subject = Optional.ofNullable(jwt.getSubject())
            .filter(value -> !value.isBlank())
            .orElseThrow(() -> new IllegalArgumentException("JWT subject is required"));

        IdentityClaims claims = new IdentityClaims(
            issuer,
            subject,
            jwt.getClaimAsString("email"),
            jwt.getClaimAsString("phone_number"),
            Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")),
            firstNonBlank(jwt.getClaimAsString("name"), jwt.getClaimAsString("preferred_username")),
            firstNonBlank(jwt.getClaimAsString("locale"), "en-US")
        );
        return repository.findOrCreateCustomer(claims);
    }

    private String firstNonBlank(String first, String fallback) {
        return first != null && !first.isBlank() ? first : fallback;
    }
}

