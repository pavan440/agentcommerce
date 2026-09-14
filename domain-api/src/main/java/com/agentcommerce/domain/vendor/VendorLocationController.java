package com.agentcommerce.domain.vendor;

import java.util.List;
import java.util.UUID;

import com.agentcommerce.domain.identity.CurrentUserService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/vendor-locations")
public class VendorLocationController {

    private final VendorService vendorService;
    private final CurrentUserService currentUserService;

    public VendorLocationController(VendorService vendorService, CurrentUserService currentUserService) {
        this.vendorService = vendorService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/{locationId}")
    public ResponseEntity<VendorLocationResponse> getLocation(@PathVariable UUID locationId) {
        return vendorService.getLocation(locationId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<VendorLocationResponse>> getLocationsByCommunity(
        @RequestParam("communityCode") String communityCode
    ) {
        return ResponseEntity.ok(vendorService.getLocationsInCommunity(communityCode));
    }

    @GetMapping("/{locationId}/commerce-settings")
    public ResponseEntity<VendorCommerceSettingsResponse> getCommerceSettings(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID locationId
    ) {
        return vendorService.getCommerceSettings(currentUserService.id(jwt), locationId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{locationId}/commerce-settings")
    public ResponseEntity<Void> updateCommerceSettings(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID locationId,
        @Valid @RequestBody UpdateCommerceSettingsRequest request
    ) {
        vendorService.updateCommerceSettings(currentUserService.id(jwt), locationId, request);
        return ResponseEntity.noContent().build();
    }
}
