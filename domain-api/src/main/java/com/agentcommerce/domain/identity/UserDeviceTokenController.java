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
@RequestMapping("/v1/me/device-tokens")
class UserDeviceTokenController {

    private final CurrentUserService currentUserService;
    private final UserProfileService userProfileService;

    UserDeviceTokenController(
        CurrentUserService currentUserService,
        UserProfileService userProfileService
    ) {
        this.currentUserService = currentUserService;
        this.userProfileService = userProfileService;
    }

    @GetMapping
    List<DeviceTokenResponse> deviceTokens(@AuthenticationPrincipal Jwt jwt) {
        return userProfileService.getDeviceTokens(currentUserService.id(jwt));
    }

    @PostMapping
    ResponseEntity<DeviceTokenResponse> registerDeviceToken(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody RegisterDeviceTokenRequest request
    ) {
        DeviceTokenResponse response = userProfileService.registerDeviceToken(currentUserService.id(jwt), request);
        return ResponseEntity.created(URI.create("/v1/me/device-tokens/" + response.id())).body(response);
    }

    @DeleteMapping("/{tokenId}")
    ResponseEntity<Void> deleteDeviceToken(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID tokenId
    ) {
        userProfileService.deleteDeviceToken(currentUserService.id(jwt), tokenId);
        return ResponseEntity.noContent().build();
    }
}
