package com.agentcommerce.domain.vendor;

import java.util.List;

public record CreateVendorRequest(
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
    String feeTierId
) {}
