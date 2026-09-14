package com.agentcommerce.domain.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryItemResponse(UUID id, UUID vendorLocationId, String sku, String name, String description,
    String category, long priceMinor, String currency, String trackingMode, int quantityOnHand,
    int quantityReserved, boolean available, int reorderThreshold, String source, long version, Instant updatedAt) {}