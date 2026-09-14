package com.agentcommerce.domain.driver;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record ReviewDasherRequest(
    @NotBlank @Pattern(regexp = "ACTIVE|SUSPENDED")
    String status,
    @NotBlank @Pattern(regexp = "APPROVED|REJECTED")
    String backgroundCheckStatus,
    @PositiveOrZero
    long expectedVersion
) {
}