package com.agentcommerce.domain.vendor;

import java.time.Instant;

public record UpdateCommerceSettingsRequest(
    Boolean isAcceptingOrders,
    Boolean autoAcceptOrders,
    Instant pausedUntil,
    String pauseReason,
    Integer busyModePrepPaddingMinutes,
    Boolean isDeliveryEnabled,
    Boolean isPickupEnabled,
    Boolean isCurbsideEnabled,
    String orderNotificationChannel,
    Integer defaultPreparationMinutes,
    Integer minimumPreparationMinutes,
    Integer maximumPreparationMinutes,
    Long minimumOrderAmountMinor,
    Long packagingFeeMinor,
    String taxCalculationMode,
    Boolean pricesIncludeTax,
    String defaultProductTaxCode,
    long expectedVersion
) {}
