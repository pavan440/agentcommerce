package com.agentcommerce.domain.vendor;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    public VendorLocationController(VendorService vendorService) {
        this.vendorService = vendorService;
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
    public ResponseEntity<VendorCommerceSettingsResponse> getCommerceSettings(@PathVariable UUID locationId) {
        return vendorService.getCommerceSettings(locationId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{locationId}/commerce-settings")
    public ResponseEntity<Void> updateCommerceSettings(
        @PathVariable UUID locationId,
        @RequestBody UpdateCommerceSettingsRequest request
    ) {
        boolean updated = vendorService.updateCommerceSettings(locationId, request);
        if (!updated) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409 Concurrent Modification
        }
        return ResponseEntity.noContent().build();
    }
}
