package com.agentcommerce.domain.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateEmergencyContactRequest(
    @NotBlank @Size(max = 255)
    String contactName,
    @Size(max = 64)
    String relationship,
    @NotBlank @Size(max = 32) @Pattern(regexp = "\\+[1-9][0-9]{7,14}")
    String phone
) {
}
