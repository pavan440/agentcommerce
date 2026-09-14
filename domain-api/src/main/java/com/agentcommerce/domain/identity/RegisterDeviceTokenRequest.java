package com.agentcommerce.domain.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterDeviceTokenRequest(
    @NotBlank @Size(max = 512)
    String token,
    @NotBlank @Pattern(regexp = "IOS|ANDROID|WEB")
    String platform
) {
}
