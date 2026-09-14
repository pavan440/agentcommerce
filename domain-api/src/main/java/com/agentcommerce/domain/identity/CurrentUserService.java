package com.agentcommerce.domain.identity;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final CustomerIdentityService identityService;

    CurrentUserService(CustomerIdentityService identityService) {
        this.identityService = identityService;
    }

    public CustomerProfileResponse profile(Jwt jwt) {
        return identityService.findOrProvision(jwt);
    }

    public UUID id(Jwt jwt) {
        return profile(jwt).id();
    }
}
