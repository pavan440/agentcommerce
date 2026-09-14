package com.agentcommerce.domain.vendor;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateVendorLocationRequest(
    @NotBlank @Size(max = 32)
    String communityCode,
    @NotBlank @Size(max = 255)
    String name,
    @NotBlank @Size(max = 120) @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*")
    String slug,
    @NotBlank @Size(max = 255)
    String addressLine1,
    @Size(max = 255)
    String addressLine2,
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
    @NotBlank @Size(max = 64)
    String timezone,
    @Size(max = 512)
    String logoUrl,
    @Size(max = 512)
    String bannerUrl,
    @Size(max = 512)
    String websiteUrl,
    @Size(max = 255)
    String googlePlaceId,
    @Email @Size(max = 320)
    String contactEmail,
    @Size(max = 32)
    String contactPhone,
    @Size(max = 2000)
    String driverPickupInstructions,
    @Size(max = 2000)
    String customerPickupInstructions,
    @Size(max = 2000)
    String parkingInstructions
) {
}
