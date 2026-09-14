package com.agentcommerce.domain.vendor;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.agentcommerce.domain.identity.CurrentUserService;

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
@RequestMapping("/v1/vendors")
public class VendorController {

    private final VendorService vendorService;
    private final CurrentUserService currentUserService;

    public VendorController(VendorService vendorService, CurrentUserService currentUserService) {
        this.vendorService = vendorService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<VendorResponse> createVendor(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CreateVendorRequest request
    ) {
        UUID userId = currentUserService.id(jwt);
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
        return ResponseEntity.ok(vendorService.getVendorsForUser(currentUserService.id(jwt)));
    }

    @PostMapping("/{vendorId}/locations")
    public ResponseEntity<VendorLocationResponse> createLocation(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID vendorId,
        @Valid @RequestBody CreateVendorLocationRequest request
    ) {
        VendorLocationResponse response = vendorService.createLocation(currentUserService.id(jwt), vendorId, request);
        return ResponseEntity.created(URI.create("/v1/vendor-locations/" + response.id())).body(response);
    }
}
