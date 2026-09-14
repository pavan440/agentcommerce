package com.agentcommerce.domain.driver;

import jakarta.validation.constraints.PositiveOrZero;

public record UpdateDasherAvailabilityRequest(
    boolean online,
    @PositiveOrZero long expectedVersion
) {
}