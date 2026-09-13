package com.agentcommerce.domain.vendor;

import java.net.URI;
import java.util.List;
import java.util.UUID;

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
@RequestMapping("/v1/vendors")
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @PostMapping
    public ResponseEntity<VendorResponse> createVendor(
        @AuthenticationPrincipal Jwt jwt,
        @RequestBody CreateVendorRequest request
    ) {
        UUID userId = extractUserId(jwt);
        VendorResponse response = vendorService.createVendor(userId, request);
        return ResponseEntity.created(URI.create("/v1/vendors/" + response.id())).body(response);
    }

    @GetMapping("/{vendorId}")
    public ResponseEntity<VendorResponse> getVendor(@PathVariable UUID vendorId) {
        return vendorService.getVendor(vendorId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/me")
    public ResponseEntity<List<VendorResponse>> getMyVendors(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = extractUserId(jwt);
        return ResponseEntity.ok(vendorService.getVendorsForUser(userId));
    }

    @PostMapping("/{vendorId}/locations")
    public ResponseEntity<VendorLocationResponse> createLocation(
        @PathVariable UUID vendorId,
        @RequestBody CreateVendorLocationRequest request
    ) {
        VendorLocationResponse response = vendorService.createLocation(vendorId, request);
        return ResponseEntity.created(URI.create("/v1/vendor-locations/" + response.id())).body(response);
    }

    private UUID extractUserId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            // Fallback for unauthenticated dev testing
            return UUID.fromString("00000000-0000-0000-0000-000000000001");
        }
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            return UUID.nameUUIDFromBytes(jwt.getSubject().getBytes());
        }
    }
}
