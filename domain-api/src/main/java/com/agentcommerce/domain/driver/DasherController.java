package com.agentcommerce.domain.driver;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.agentcommerce.domain.identity.CurrentUserService;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/dashers/me")
class DasherController {

    private final CurrentUserService currentUserService;
    private final DasherService dasherService;

    DasherController(CurrentUserService currentUserService, DasherService dasherService) {
        this.currentUserService = currentUserService;
        this.dasherService = dasherService;
    }

    @PostMapping("/onboarding")
    ResponseEntity<DasherProfileResponse> createProfile(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CreateDasherProfileRequest request
    ) {
        DasherProfileResponse response = dasherService.createProfile(currentUserService.id(jwt), request);
        return ResponseEntity.created(URI.create("/v1/dashers/me")).body(response);
    }

    @GetMapping
    DasherProfileResponse profile(@AuthenticationPrincipal Jwt jwt) {
        return dasherService.getProfile(currentUserService.id(jwt));
    }

    @PatchMapping
    DasherProfileResponse updateProfile(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody UpdateDasherProfileRequest request
    ) {
        return dasherService.updateProfile(currentUserService.id(jwt), request);
    }

    @PatchMapping("/availability")
    DasherProfileResponse updateAvailability(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody UpdateDasherAvailabilityRequest request
    ) {
        return dasherService.updateAvailability(currentUserService.id(jwt), request);
    }

    @GetMapping("/operating-zones")
    List<DasherOperatingZoneResponse> operatingZones(@AuthenticationPrincipal Jwt jwt) {
        return dasherService.getOperatingZones(currentUserService.id(jwt));
    }

    @PostMapping("/operating-zones")
    ResponseEntity<DasherOperatingZoneResponse> createOperatingZone(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CreateDasherOperatingZoneRequest request
    ) {
        DasherOperatingZoneResponse response =
            dasherService.createOperatingZone(currentUserService.id(jwt), request);
        return ResponseEntity.created(
            URI.create("/v1/dashers/me/operating-zones/" + response.id())
        ).body(response);
    }

    @DeleteMapping("/operating-zones/{zoneId}")
    ResponseEntity<Void> deleteOperatingZone(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID zoneId
    ) {
        dasherService.deleteOperatingZone(currentUserService.id(jwt), zoneId);
        return ResponseEntity.noContent().build();
    }
}