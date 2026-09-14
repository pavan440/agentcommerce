package com.agentcommerce.domain.identity;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateCustomerProfileRequest(
    @Size(max = 512)
    String profileImageUrl,
    @Size(max = 255)
    String displayName,
    @Size(max = 32)
    String locale,
    @Size(max = 64)
    String timezone,
    @Size(max = 30)
    List<@NotBlank @Size(max = 64) String> dietaryRestrictions,
    @Size(max = 30)
    List<@NotBlank @Size(max = 64) String> allergens,
    @Pattern(regexp = "PREFERENCE_BASED|APPROVAL_REQUIRED")
    String substitutionMode,
    Boolean autoOrderEnabled,
    @PositiveOrZero
    Long autoOrderMaxAmountMinor,
    @DecimalMin("0.00") @DecimalMax("100.00")
    BigDecimal defaultTipPercentage,
    @PositiveOrZero
    long expectedVersion
) {
}
