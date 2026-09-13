package com.agentcommerce.domain.vendor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VendorResponse(
    UUID id,
    String legalName,
    String displayName,
    String slug,
    String category,
    List<String> tags,
    String logoUrl,
    String bannerUrl,
    String description,
    String supportEmail,
    String supportPhone,
    String defaultCurrency,
    String feeTierId,
    String status,
    UUID createdByUserId,
    Instant createdAt,
    Instant updatedAt,
    long version
) {}
