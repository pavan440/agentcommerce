package com.agentcommerce.domain.identity;

import java.time.Instant;
import java.util.UUID;

public record CustomerAddressResponse(
    UUID id,
    String label,
    String addressLine1,
    String addressLine2,
    String buildingName,
    String gateCode,
    String locality,
    String administrativeArea,
    String postalCode,
    String countryCode,
    String formattedAddress,
    double latitude,
    double longitude,
    String deliveryInstructions,
    boolean isDefault,
    Instant createdAt,
    Instant updatedAt
) {
}
