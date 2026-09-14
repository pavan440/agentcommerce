package com.agentcommerce.domain.driver;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateDasherProfileRequest(
    @Pattern(regexp = "CAR|BICYCLE|SCOOTER|FOOT")
    String vehicleType,
    @Size(max = 64)
    String vehicleMake,
    @Size(max = 64)
    String vehicleModel,
    @Size(max = 32)
    String vehicleColor,
    @Size(max = 32)
    String licensePlate,
    @Size(max = 64)
    String licenseNumber,
    @Min(1) @Max(10)
    Integer maxActivePickups,
    @PositiveOrZero
    long expectedVersion
) {
}