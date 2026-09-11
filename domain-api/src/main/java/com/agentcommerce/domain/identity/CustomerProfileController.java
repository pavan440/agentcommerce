package com.agentcommerce.domain.identity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/me")
public class CustomerProfileController {

    private final CustomerIdentityService identityService;

    CustomerProfileController(CustomerIdentityService identityService) {
        this.identityService = identityService;
    }

    @GetMapping
    CustomerProfileResponse currentCustomer(@AuthenticationPrincipal Jwt jwt) {
        return identityService.findOrProvision(jwt);
    }
}
