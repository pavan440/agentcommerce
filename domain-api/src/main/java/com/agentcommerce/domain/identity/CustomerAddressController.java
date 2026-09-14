package com.agentcommerce.domain.identity;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/me/addresses")
class CustomerAddressController {

    private final CurrentUserService currentUserService;
    private final UserProfileService userProfileService;

    CustomerAddressController(
        CurrentUserService currentUserService,
        UserProfileService userProfileService
    ) {
        this.currentUserService = currentUserService;
        this.userProfileService = userProfileService;
    }

    @GetMapping
    List<CustomerAddressResponse> addresses(@AuthenticationPrincipal Jwt jwt) {
        return userProfileService.getAddresses(currentUserService.id(jwt));
    }

    @PostMapping
    ResponseEntity<CustomerAddressResponse> createAddress(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CreateCustomerAddressRequest request
    ) {
        CustomerAddressResponse response = userProfileService.createAddress(currentUserService.id(jwt), request);
        return ResponseEntity.created(URI.create("/v1/me/addresses/" + response.id())).body(response);
    }

    @DeleteMapping("/{addressId}")
    ResponseEntity<Void> deleteAddress(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID addressId
    ) {
        userProfileService.deleteAddress(currentUserService.id(jwt), addressId);
        return ResponseEntity.noContent().build();
    }
}
