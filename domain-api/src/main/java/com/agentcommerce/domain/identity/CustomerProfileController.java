package com.agentcommerce.domain.identity;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/me")
public class CustomerProfileController {

    private final CurrentUserService currentUserService;
    private final UserProfileService userProfileService;

    CustomerProfileController(
        CurrentUserService currentUserService,
        UserProfileService userProfileService
    ) {
        this.currentUserService = currentUserService;
        this.userProfileService = userProfileService;
    }

    @GetMapping
    CustomerProfileResponse currentCustomer(@AuthenticationPrincipal Jwt jwt) {
        return currentUserService.profile(jwt);
    }

    @PatchMapping
    CustomerProfileResponse updateCustomer(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody UpdateCustomerProfileRequest request
    ) {
        return userProfileService.updateProfile(currentUserService.id(jwt), request);
    }
}
