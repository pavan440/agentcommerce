package com.agentcommerce.domain.identity;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCustomerAddressRequest(
    @NotBlank @Size(max = 64)
    String label,
    @NotBlank @Size(max = 255)
    String addressLine1,
    @Size(max = 255)
    String addressLine2,
    @Size(max = 120)
    String buildingName,
    @Size(max = 32)
    String gateCode,
    @NotBlank @Size(max = 120)
    String locality,
    @NotBlank @Size(max = 120)
    String administrativeArea,
    @NotBlank @Size(max = 32)
    String postalCode,
    @Pattern(regexp = "[A-Z]{2}")
    String countryCode,
    @NotBlank @Size(max = 512)
    String formattedAddress,
    @DecimalMin("-90.0") @DecimalMax("90.0")
    double latitude,
    @DecimalMin("-180.0") @DecimalMax("180.0")
    double longitude,
    @Size(max = 2000)
    String deliveryInstructions,
    boolean isDefault
) {
}
