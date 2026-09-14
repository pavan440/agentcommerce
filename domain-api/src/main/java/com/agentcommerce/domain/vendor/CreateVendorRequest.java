package com.agentcommerce.domain.vendor;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateVendorRequest(
    @NotBlank @Size(max = 255)
    String legalName,
    @NotBlank @Size(max = 255)
    String displayName,
    @NotBlank @Size(max = 120) @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*")
    String slug,
    @Size(max = 64)
    String category,
    @Size(max = 20)
    List<@NotBlank @Size(max = 64) String> tags,
    @Size(max = 512)
    String logoUrl,
    @Size(max = 512)
    String bannerUrl,
    @Size(max = 4000)
    String description,
    @Email @Size(max = 320)
    String supportEmail,
    @Size(max = 32)
    String supportPhone,
    @Pattern(regexp = "[A-Z]{3}")
    String defaultCurrency,
    @Size(max = 64)
    String feeTierId
) {
}
