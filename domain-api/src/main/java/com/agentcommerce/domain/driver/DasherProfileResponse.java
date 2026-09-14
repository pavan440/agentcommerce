package com.agentcommerce.domain.driver;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DasherProfileResponse(
    UUID userId,
    String status,
    String vehicleType,
    String vehicleMake,
    String vehicleModel,
    String vehicleColor,
    String licensePlate,
    String licenseNumberLast4,
    String backgroundCheckStatus,
    boolean online,
    int maxActivePickups,
    BigDecimal averageRating,
    int totalDeliveries,
    long version,
    Instant createdAt,
    Instant updatedAt
) {
}