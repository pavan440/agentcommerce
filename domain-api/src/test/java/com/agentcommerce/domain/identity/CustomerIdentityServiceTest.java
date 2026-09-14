package com.agentcommerce.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class CustomerIdentityServiceTest {

    @Test
    void mapsStableOidcIdentityAndProfileClaims() {
        CustomerIdentityRepository repository = mock(CustomerIdentityRepository.class);
        CustomerIdentityService service = new CustomerIdentityService(repository);
        CustomerProfileResponse expected = new CustomerProfileResponse(
            UUID.randomUUID(),
            "customer@example.com",
            null,
            null,
            "Ada Customer",
            "en-US",
            "UTC",
            List.of(),
            List.of(),
            "APPROVAL_REQUIRED",
            false,
            0L,
            new BigDecimal("15.00"),
            0L,
            Set.of("CUSTOMER")
        );
        when(repository.findOrCreateCustomer(argThat(claims ->
            claims.issuer().equals("https://identity.example.com")
                && claims.subject().equals("customer-123")
                && claims.emailVerified()
        ))).thenReturn(expected);

        Jwt jwt = new Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(300),
            Map.of("alg", "RS256"),
            Map.of(
                "iss", "https://identity.example.com",
                "sub", "customer-123",
                "email", "customer@example.com",
                "email_verified", true,
                "name", "Ada Customer",
                "locale", "en-US"
            )
        );

        assertThat(service.findOrProvision(jwt)).isEqualTo(expected);
    }
}