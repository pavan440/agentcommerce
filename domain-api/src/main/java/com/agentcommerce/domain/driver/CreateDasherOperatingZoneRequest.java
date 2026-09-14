package com.agentcommerce.domain.driver;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDasherOperatingZoneRequest(
    @NotBlank @Size(max = 120)
    String zoneName,
    @NotNull @Size(min = 3, max = 1000)
    List<@Valid DriverZonePoint> boundary
) {
}