package com.agentcommerce.domain.vendor;

public record CreateVendorLocationRequest(
    String communityCode,
    String name,
    String slug,
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
    String contactEmail,
    String contactPhone,
    String driverPickupInstructions,
    String customerPickupInstructions,
    String parkingInstructions
) {}
