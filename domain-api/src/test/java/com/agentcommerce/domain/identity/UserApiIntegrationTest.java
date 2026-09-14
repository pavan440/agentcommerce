package com.agentcommerce.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "RUN_DB_INTEGRATION_TESTS", matches = "true")
class UserApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void updatesProfileAndMaintainsOneDefaultOwnedAddress() throws Exception {
        String subject = "profile-" + UUID.randomUUID();
        String otherSubject = "other-" + UUID.randomUUID();

        mockMvc.perform(get("/v1/me").with(customer(subject)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("Integration Customer"))
            .andExpect(jsonPath("$.version").value(0))
            .andExpect(jsonPath("$.roles[0]").value("CUSTOMER"));

        mockMvc.perform(patch("/v1/me")
                .with(customer(subject))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "profileImageUrl": "https://cdn.example.com/profile.png",
                      "displayName": "Ada Customer",
                      "locale": "en-US",
                      "timezone": "America/Los_Angeles",
                      "dietaryRestrictions": ["VEGETARIAN"],
                      "allergens": ["PEANUTS"],
                      "substitutionMode": "PREFERENCE_BASED",
                      "autoOrderEnabled": true,
                      "autoOrderMaxAmountMinor": 7500,
                      "defaultTipPercentage": 18.50,
                      "expectedVersion": 0
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("Ada Customer"))
            .andExpect(jsonPath("$.dietaryRestrictions[0]").value("VEGETARIAN"))
            .andExpect(jsonPath("$.autoOrderEnabled").value(true))
            .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(patch("/v1/me")
                .with(customer(subject))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"displayName": "Stale Update", "expectedVersion": 0}
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("reload")));

        mockMvc.perform(post("/v1/me/addresses")
                .with(customer(subject))
                .contentType(MediaType.APPLICATION_JSON)
                .content(addressJson("Home", "100 Pine Street", false)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.label").value("HOME"))
            .andExpect(jsonPath("$.isDefault").value(true));

        mockMvc.perform(post("/v1/me/addresses")
                .with(customer(subject))
                .contentType(MediaType.APPLICATION_JSON)
                .content(addressJson("Work", "200 Pine Street", true)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.label").value("WORK"))
            .andExpect(jsonPath("$.isDefault").value(true));

        UUID userId = userId(subject);
        UUID workAddressId = jdbcClient.sql("""
                SELECT id FROM customer_addresses
                WHERE user_id = :userId AND label = 'WORK'
                """)
            .param("userId", userId)
            .query(UUID.class)
            .single();

        mockMvc.perform(delete("/v1/me/addresses/{addressId}", workAddressId)
                .with(customer(otherSubject)))
            .andExpect(status().isNotFound());

        mockMvc.perform(delete("/v1/me/addresses/{addressId}", workAddressId)
                .with(customer(subject)))
            .andExpect(status().isNoContent());

        Long defaultCount = jdbcClient.sql("""
                SELECT count(*) FROM customer_addresses
                WHERE user_id = :userId AND is_default = TRUE
                """)
            .param("userId", userId)
            .query(Long.class)
            .single();
        assertThat(defaultCount).isEqualTo(1L);
    }

    @Test
    void protectsDeviceTokensContactsAndRecordsLatestConsent() throws Exception {
        String subject = "preferences-" + UUID.randomUUID();
        String otherSubject = "other-" + UUID.randomUUID();
        String token = "device-" + UUID.randomUUID();

        mockMvc.perform(get("/v1/me").with(customer(subject)))
            .andExpect(status().isOk());
        mockMvc.perform(get("/v1/me").with(customer(otherSubject)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/v1/me/device-tokens")
                .with(customer(subject))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"token": "%s", "platform": "IOS"}
                    """.formatted(token)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.platform").value("IOS"))
            .andExpect(jsonPath("$.token").doesNotExist());

        mockMvc.perform(post("/v1/me/device-tokens")
                .with(customer(otherSubject))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"token": "%s", "platform": "ANDROID"}
                    """.formatted(token)))
            .andExpect(status().isConflict());

        mockMvc.perform(post("/v1/me/emergency-contacts")
                .with(customer(subject))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "contactName": "Grace Customer",
                      "relationship": "SPOUSE",
                      "phone": "+12065550123"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.contactName").value("Grace Customer"));

        UUID contactId = jdbcClient.sql("""
                SELECT id FROM user_emergency_contacts
                WHERE user_id = :userId AND phone = '+12065550123'
                """)
            .param("userId", userId(subject))
            .query(UUID.class)
            .single();

        mockMvc.perform(delete("/v1/me/emergency-contacts/{contactId}", contactId)
                .with(customer(otherSubject)))
            .andExpect(status().isNotFound());

        mockMvc.perform(post("/v1/me/consents/location_tracking")
                .with(customer(subject))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"granted": true, "policyVersion": "2026-09"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.consentType").value("LOCATION_TRACKING"))
            .andExpect(jsonPath("$.granted").value(true));

        mockMvc.perform(post("/v1/me/consents/LOCATION_TRACKING")
                .with(customer(subject))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"granted": false, "policyVersion": "2026-10"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.granted").value(false));

        mockMvc.perform(get("/v1/me/consents").with(customer(subject)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].policyVersion").value("2026-10"))
            .andExpect(jsonPath("$[0].granted").value(false));
    }

    private UUID userId(String subject) {
        return jdbcClient.sql("""
                SELECT user_id FROM user_identities
                WHERE issuer = 'https://identity.integration.test' AND subject = :subject
                """)
            .param("subject", subject)
            .query(UUID.class)
            .single();
    }

    private RequestPostProcessor customer(String subject) {
        return jwt().jwt(builder -> builder
            .issuer("https://identity.integration.test")
            .subject(subject)
            .claim("email", subject.toLowerCase(Locale.ROOT) + "@example.com")
            .claim("email_verified", true)
            .claim("name", "Integration Customer")
            .claim("locale", "en-US"));
    }

    private String addressJson(String label, String line1, boolean isDefault) {
        return """
            {
              "label": "%s",
              "addressLine1": "%s",
              "locality": "Seattle",
              "administrativeArea": "WA",
              "postalCode": "98101",
              "countryCode": "US",
              "formattedAddress": "%s, Seattle, WA 98101",
              "latitude": 47.6101,
              "longitude": -122.3344,
              "isDefault": %s
            }
            """.formatted(label, line1, line1, isDefault);
    }
}