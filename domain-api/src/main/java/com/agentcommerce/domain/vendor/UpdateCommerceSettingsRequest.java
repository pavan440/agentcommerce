package com.agentcommerce.domain.vendor;

import java.time.Instant;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateCommerceSettingsRequest(
    Boolean isAcceptingOrders,
    Boolean autoAcceptOrders,
    Instant pausedUntil,
    @Size(max = 255)
    String pauseReason,
    @PositiveOrZero
    Integer busyModePrepPaddingMinutes,
    Boolean isDeliveryEnabled,
    Boolean isPickupEnabled,
    Boolean isCurbsideEnabled,
    @Pattern(regexp = "TABLET|POS|WEBHOOK|SMS|EMAIL")
    String orderNotificationChannel,
    @Positive
    Integer defaultPreparationMinutes,
    @Positive
    Integer minimumPreparationMinutes,
    @Positive
    Integer maximumPreparationMinutes,
    @PositiveOrZero
    Long minimumOrderAmountMinor,
    @PositiveOrZero
    Long packagingFeeMinor,
    @Pattern(regexp = "PROVIDER|INCLUSIVE|EXCLUSIVE")
    String taxCalculationMode,
    Boolean pricesIncludeTax,
    @Size(max = 120)
    String defaultProductTaxCode,
    @PositiveOrZero
    long expectedVersion
) {
}
