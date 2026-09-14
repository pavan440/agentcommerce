package com.agentcommerce.domain.driver;

import java.time.Instant;
import java.util.UUID;

public record DasherOperatingZoneResponse(
    UUID id,
    String zoneName,
    String areaGeoJson,
    Instant createdAt
) {
}