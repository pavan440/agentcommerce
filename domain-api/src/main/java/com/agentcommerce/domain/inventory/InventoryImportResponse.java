package com.agentcommerce.domain.inventory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InventoryImportResponse(UUID id, UUID vendorLocationId, String fileName, String status,
    int totalRows, int validRows, int invalidRows, long version, Instant createdAt,
    List<InventoryImportRowResponse> rows) {}