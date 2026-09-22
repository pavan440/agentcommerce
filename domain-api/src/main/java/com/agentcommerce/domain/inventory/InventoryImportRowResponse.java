package com.agentcommerce.domain.inventory;

import java.util.UUID;

public record InventoryImportRowResponse(UUID id, int rowNumber, String sku, String name, String imageUrl, String category,
    Long priceMinor, String currency, Integer quantityOnHand, Boolean available, Integer reorderThreshold,
    boolean valid, boolean included, String errorMessage) {}
