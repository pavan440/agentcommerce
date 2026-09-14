package com.agentcommerce.domain.identity;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/me/consents")
class UserConsentController {

    private final CurrentUserService currentUserService;
    private final UserProfileService userProfileService;

    UserConsentController(
        CurrentUserService currentUserService,
        UserProfileService userProfileService
    ) {
        this.currentUserService = currentUserService;
        this.userProfileService = userProfileService;
    }

    @GetMapping
    List<ConsentResponse> currentConsents(@AuthenticationPrincipal Jwt jwt) {
        return userProfileService.getCurrentConsents(currentUserService.id(jwt));
    }

    @PostMapping("/{consentType}")
    ResponseEntity<ConsentResponse> recordConsent(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String consentType,
        @Valid @RequestBody RecordConsentRequest request
    ) {
        ConsentResponse response =
            userProfileService.recordConsent(currentUserService.id(jwt), consentType, request);
        return ResponseEntity.created(URI.create("/v1/me/consents/" + response.id())).body(response);
    }
}
