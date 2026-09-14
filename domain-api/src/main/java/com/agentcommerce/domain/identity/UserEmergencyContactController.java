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
@RequestMapping("/v1/me/emergency-contacts")
class UserEmergencyContactController {

    private final CurrentUserService currentUserService;
    private final UserProfileService userProfileService;

    UserEmergencyContactController(
        CurrentUserService currentUserService,
        UserProfileService userProfileService
    ) {
        this.currentUserService = currentUserService;
        this.userProfileService = userProfileService;
    }

    @GetMapping
    List<EmergencyContactResponse> emergencyContacts(@AuthenticationPrincipal Jwt jwt) {
        return userProfileService.getEmergencyContacts(currentUserService.id(jwt));
    }

    @PostMapping
    ResponseEntity<EmergencyContactResponse> createEmergencyContact(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CreateEmergencyContactRequest request
    ) {
        EmergencyContactResponse response =
            userProfileService.createEmergencyContact(currentUserService.id(jwt), request);
        return ResponseEntity.created(URI.create("/v1/me/emergency-contacts/" + response.id())).body(response);
    }

    @DeleteMapping("/{contactId}")
    ResponseEntity<Void> deleteEmergencyContact(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID contactId
    ) {
        userProfileService.deleteEmergencyContact(currentUserService.id(jwt), contactId);
        return ResponseEntity.noContent().build();
    }
}
