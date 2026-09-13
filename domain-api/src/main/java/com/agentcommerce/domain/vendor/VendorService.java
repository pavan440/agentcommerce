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

    public VendorLocationResponse createLocation(UUID vendorId, CreateVendorLocationRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Location name cannot be blank");
        }
        if (request.communityCode() == null || request.communityCode().isBlank()) {
            throw new IllegalArgumentException("Community code is required for hyperlocal store partitioning");
        }
        return vendorRepository.createVendorLocation(vendorId, request);
    }

    public Optional<VendorLocationResponse> getLocation(UUID locationId) {
        return vendorRepository.findLocationById(locationId);
    }

    public List<VendorLocationResponse> getLocationsInCommunity(String communityCode) {
        return vendorRepository.findLocationsByCommunity(communityCode);
    }

    public Optional<VendorCommerceSettingsResponse> getCommerceSettings(UUID locationId) {
        return vendorRepository.findCommerceSettings(locationId);
    }

    public boolean updateCommerceSettings(UUID locationId, UpdateCommerceSettingsRequest request) {
        return vendorRepository.updateCommerceSettings(locationId, request);
    }
}
