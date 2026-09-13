package com.agentcommerce.domain.vendor;

import java.time.Instant;
import java.util.UUID;

public record VendorLocationResponse(
    UUID id,
    UUID vendorId,
    String communityCode,
    String name,
    String slug,
    String status,
    String addressLine1,
    String addressLine2,
    String locality,
    String administrativeArea,
    String postalCode,
    String countryCode,
    String formattedAddress,
    double latitude,
    double longitude,
    String timezone,
    String logoUrl,
    String bannerUrl,
    String websiteUrl,
    String googlePlaceId,
    double averageRating,
    int reviewCount,
    String contactEmail,
    String contactPhone,
    String driverPickupInstructions,
    String customerPickupInstructions,
    String parkingInstructions,
    Instant createdAt,
    Instant updatedAt,
    long version
) {}
