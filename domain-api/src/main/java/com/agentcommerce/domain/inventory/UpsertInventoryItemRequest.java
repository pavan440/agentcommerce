package com.agentcommerce.domain.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpsertInventoryItemRequest(
    @NotBlank @Size(max = 120) String sku,
    @NotBlank @Size(max = 255) String name,
    @Size(max = 4000) String description,
    @Size(max = 120) String category,
    @Min(0) long priceMinor,
    @Pattern(regexp = "^[A-Z]{3}$") String currency,
    @Pattern(regexp = "^(QUANTITY|AVAILABILITY_ONLY)$") String trackingMode,
    @Min(0) int quantityOnHand,
    boolean available,
    @Min(0) int reorderThreshold,
    Long version) {}