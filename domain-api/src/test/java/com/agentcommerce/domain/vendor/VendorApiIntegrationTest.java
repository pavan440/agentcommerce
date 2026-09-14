package com.agentcommerce.domain.vendor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class VendorApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void completesVendorOnboardingAndProtectsLocationSettings() throws Exception {
        String ownerSubject = "owner-" + UUID.randomUUID();
        String vendorSlug = uniqueSlug("vendor");
        String locationSlug = uniqueSlug("location");

        mockMvc.perform(post("/v1/vendors")
                .with(customer(ownerSubject))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "legalName": "Neighborhood Foods LLC",
                      "displayName": "Neighborhood Foods",
                      "slug": "%s",
                      "category": "restaurant",
                      "tags": ["local", "family-owned"],
                      "supportEmail": "owner@example.com",
                      "defaultCurrency": "USD"
                    }
                    """.formatted(vendorSlug)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/v1/vendors/")))
            .andExpect(jsonPath("$.slug").value(vendorSlug))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.category").value("RESTAURANT"));

        UUID vendorId = jdbcClient.sql("SELECT id FROM vendors WHERE slug = :slug")
            .param("slug", vendorSlug)
            .query(UUID.class)
            .single();

        Long ownerRoleCount = jdbcClient.sql("""
                SELECT count(*)
                FROM vendor_memberships
                WHERE vendor_id = :vendorId AND role = 'OWNER' AND status = 'ACTIVE'
                """)
            .param("vendorId", vendorId)
            .query(Long.class)
            .single();
        assertThat(ownerRoleCount).isEqualTo(1L);

        mockMvc.perform(post("/v1/vendors/{vendorId}/locations", vendorId)
                .with(customer(ownerSubject))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityCode": "sea-capitol-hill",
                      "name": "Pine Street",
                      "slug": "%s",
                      "addressLine1": "100 Pine Street",
                      "locality": "Seattle",
                      "administrativeArea": "WA",
                      "postalCode": "98101",
                      "countryCode": "US",
                      "formattedAddress": "100 Pine Street, Seattle, WA 98101",
                      "latitude": 47.6101,
                      "longitude": -122.3344,
                      "timezone": "America/Los_Angeles",
                      "contactEmail": "store@example.com"
                    }
                    """.formatted(locationSlug)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.communityCode").value("SEA-CAPITOL-HILL"))
            .andExpect(jsonPath("$.status").value("DRAFT"));

        UUID locationId = jdbcClient.sql("""
                SELECT id
                FROM vendor_locations
                WHERE vendor_id = :vendorId AND slug = :slug
                """)
            .param("vendorId", vendorId)
            .param("slug", locationSlug)
            .query(UUID.class)
            .single();

        mockMvc.perform(get("/v1/vendor-locations/{locationId}/commerce-settings", locationId)
                .with(customer(ownerSubject)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(0))
            .andExpect(jsonPath("$.currency").value("USD"));

        mockMvc.perform(patch("/v1/vendor-locations/{locationId}/commerce-settings", locationId)
                .with(customer(ownerSubject))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "isAcceptingOrders": true,
                      "defaultPreparationMinutes": 25,
                      "minimumPreparationMinutes": 10,
                      "maximumPreparationMinutes": 60,
                      "expectedVersion": 0
                    }
                    """))
            .andExpect(status().isNoContent());

        mockMvc.perform(patch("/v1/vendor-locations/{locationId}/commerce-settings", locationId)
                .with(customer(ownerSubject))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "isAcceptingOrders": false,
                      "expectedVersion": 0
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("reload")));

        mockMvc.perform(get("/v1/vendor-locations/{locationId}/commerce-settings", locationId)
                .with(customer("other-" + UUID.randomUUID())))
            .andExpect(status().isForbidden());
    }

    @Test
    void rejectsInvalidVendorPayloadBeforeWriting() throws Exception {
        String invalidSlug = "Invalid Slug " + UUID.randomUUID();

        mockMvc.perform(post("/v1/vendors")
                .with(customer("invalid-" + UUID.randomUUID()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "legalName": "",
                      "displayName": "",
                      "slug": "%s",
                      "supportEmail": "not-an-email"
                    }
                    """.formatted(invalidSlug)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Request validation failed"))
            .andExpect(jsonPath("$.errors.legalName").exists())
            .andExpect(jsonPath("$.errors.slug").exists());
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

    private String uniqueSlug(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "");
    }
}
