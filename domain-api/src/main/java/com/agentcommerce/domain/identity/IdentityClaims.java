package com.agentcommerce.domain.identity;

record IdentityClaims(
    String issuer,
    String subject,
    String email,
    String phone,
    boolean emailVerified,
    String displayName,
    String locale
) {
}

