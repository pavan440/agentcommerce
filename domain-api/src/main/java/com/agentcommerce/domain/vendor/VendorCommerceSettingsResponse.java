package com.agentcommerce.domain.vendor;

import java.time.Instant;
import java.util.UUID;

public record VendorCommerceSettingsResponse(
    UUID vendorLocationId,
    String currency,
    boolean isAcceptingOrders,
    boolean autoAcceptOrders,
    Instant pausedUntil,
    String pauseReason,
    int busyModePrepPaddingMinutes,
    boolean isDeliveryEnabled,
    boolean isPickupEnabled,
    boolean isCurbsideEnabled,
    String orderNotificationChannel,
    int defaultPreparationMinutes,
    int minimumPreparationMinutes,
    int maximumPreparationMinutes,
    long minimumOrderAmountMinor,
    long packagingFeeMinor,
    String taxCalculationMode,
    boolean pricesIncludeTax,
    String defaultProductTaxCode,
    long version
) {}
