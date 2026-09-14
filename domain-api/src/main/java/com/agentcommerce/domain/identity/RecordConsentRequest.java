package com.agentcommerce.domain.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecordConsentRequest(
    boolean granted,
    @NotBlank @Size(max = 64)
    String policyVersion
) {
}
