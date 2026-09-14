package com.agentcommerce.domain.vendor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;

    public VendorService(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    public VendorResponse createVendor(UUID userId, CreateVendorRequest request) {
        if (request.displayName() == null || request.displayName().isBlank()) {
            throw new IllegalArgumentException("Vendor display name cannot be blank");
        }
        if (request.slug() == null || request.slug().isBlank()) {
            throw new IllegalArgumentException("Vendor slug cannot be blank");
        }
        return vendorRepository.createVendor(userId, request);
    }

    public Optional<VendorResponse> getVendor(UUID vendorId) {
        return vendorRepository.findVendorById(vendorId);
    }

    public List<VendorResponse> getVendorsForUser(UUID userId) {
        return vendorRepository.findVendorsByUserId(userId);
    }

    public VendorLocationResponse createLocation(
        UUID userId,
        UUID vendorId,
        CreateVendorLocationRequest request
    ) {
        if (!vendorRepository.canCreateLocations(userId, vendorId)) {
            throw new VendorAccessDeniedException("An active owner or administrator membership is required");
        }
        return vendorRepository.createVendorLocation(vendorId, request);
    }

    public Optional<VendorLocationResponse> getLocation(UUID locationId) {
        return vendorRepository.findLocationById(locationId);
    }

    public List<VendorLocationResponse> getLocationsInCommunity(String communityCode) {
        if (communityCode == null || communityCode.isBlank()) {
            throw new IllegalArgumentException("Community code is required");
        }
        return vendorRepository.findLocationsByCommunity(communityCode.trim());
    }

    public Optional<VendorCommerceSettingsResponse> getCommerceSettings(UUID userId, UUID locationId) {
        requireLocationManager(userId, locationId);
        return vendorRepository.findCommerceSettings(locationId);
    }

    public void updateCommerceSettings(
        UUID userId,
        UUID locationId,
        UpdateCommerceSettingsRequest request
    ) {
        requireLocationManager(userId, locationId);
        validatePreparationRange(request);
        if (!vendorRepository.updateCommerceSettings(locationId, request)) {
            throw new VendorConflictException("Commerce settings changed; reload the resource and retry");
        }
    }

    private void requireLocationManager(UUID userId, UUID locationId) {
        if (!vendorRepository.canManageLocation(userId, locationId)) {
            throw new VendorAccessDeniedException("An active location manager membership is required");
        }
    }

    private void validatePreparationRange(UpdateCommerceSettingsRequest request) {
        Integer minimum = request.minimumPreparationMinutes();
        Integer standard = request.defaultPreparationMinutes();
        Integer maximum = request.maximumPreparationMinutes();

        if (minimum != null && standard != null && minimum > standard) {
            throw new IllegalArgumentException("Minimum preparation time cannot exceed the default");
        }
        if (standard != null && maximum != null && standard > maximum) {
            throw new IllegalArgumentException("Default preparation time cannot exceed the maximum");
        }
        if (minimum != null && maximum != null && minimum > maximum) {
            throw new IllegalArgumentException("Minimum preparation time cannot exceed the maximum");
        }
    }
}
