package com.agentcommerce.domain.driver;

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
class DasherApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void completesOnboardingVerificationAndAvailabilityLifecycle() throws Exception {
        String dasherSubject = "dasher-" + UUID.randomUUID();
        String operatorSubject = "operator-" + UUID.randomUUID();
        String otherSubject = "other-" + UUID.randomUUID();

        mockMvc.perform(get("/v1/dashers/me").with(customer(dasherSubject)))
            .andExpect(status().isNotFound());

        mockMvc.perform(post("/v1/dashers/me/onboarding")
                .with(customer(dasherSubject))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "vehicleType": "CAR",
                      "vehicleMake": "Toyota",
                      "vehicleModel": "Camry",
                      "vehicleColor": "Silver",
                      "licensePlate": "SEA1234",
                      "licenseNumber": "WA-DL-12345678",
                      "maxActivePickups": 4
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"))
            .andExpect(jsonPath("$.backgroundCheckStatus").value("PENDING"))
            .andExpect(jsonPath("$.licenseNumberLast4").value("5678"))
            .andExpect(jsonPath("$.licenseNumber").doesNotExist())
            .andExpect(jsonPath("$.version").value(0));

        UUID dasherId = userId(dasherSubject);
        Long driverRoleCount = jdbcClient.sql("""
                SELECT count(*) FROM user_roles
                WHERE user_id = :userId AND role = 'DRIVER'
                """)
            .param("userId", dasherId)
            .query(Long.class)
            .single();
        assertThat(driverRoleCount).isEqualTo(1L);

        mockMvc.perform(post("/v1/dashers/me/onboarding")
                .with(customer(dasherSubject))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"vehicleType": "FOOT"}
                    """))
            .andExpect(status().isConflict());

        mockMvc.perform(patch("/v1/dashers/me/availability")
                .with(customer(dasherSubject))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"online": true, "expectedVersion": 0}
                    """))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/v1/dashers/me/operating-zones")
                .with(customer(dasherSubject))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "zoneName": "Downtown Seattle",
                      "boundary": [
                        {"latitude": 47.6200, "longitude": -122.3500},
                        {"latitude": 47.6200, "longitude": -122.3200},
                        {"latitude": 47.5900, "longitude": -122.3200},
                        {"latitude": 47.5900, "longitude": -122.3500}
                      ]
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.zoneName").value("Downtown Seattle"))
            .andExpect(jsonPath("$.areaGeoJson").value(org.hamcrest.Matchers.containsString("MultiPolygon")));

        UUID zoneId = jdbcClient.sql("""
                SELECT id FROM driver_operating_zones
                WHERE driver_user_id = :userId
                """)
            .param("userId", dasherId)
            .query(UUID.class)
            .single();

        mockMvc.perform(get("/v1/me").with(customer(operatorSubject)))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/v1/operators/dashers/{dasherId}/verification", dasherId)
                .with(customer(operatorSubject))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "ACTIVE",
                      "backgroundCheckStatus": "APPROVED",
                      "expectedVersion": 0
                    }
                    """))
            .andExpect(status().isForbidden());

        UUID operatorId = userId(operatorSubject);
        jdbcClient.sql("""
                INSERT INTO user_roles (user_id, role) VALUES (:userId, 'OPERATOR')
                """)
            .param("userId", operatorId)
            .update();

        mockMvc.perform(patch("/v1/operators/dashers/{dasherId}/verification", dasherId)
                .with(customer(operatorSubject))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "ACTIVE",
                      "backgroundCheckStatus": "APPROVED",
                      "expectedVersion": 0
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.backgroundCheckStatus").value("APPROVED"))
            .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(patch("/v1/dashers/me/availability")
                .with(customer(dasherSubject))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"online": true, "expectedVersion": 1}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.online").value(true))
            .andExpect(jsonPath("$.version").value(2));

        mockMvc.perform(delete("/v1/dashers/me/operating-zones/{zoneId}", zoneId)
                .with(customer(dasherSubject)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("online dasher")));

        mockMvc.perform(patch("/v1/dashers/me/availability")
                .with(customer(dasherSubject))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"online": false, "expectedVersion": 2}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.online").value(false))
            .andExpect(jsonPath("$.version").value(3));

        mockMvc.perform(delete("/v1/dashers/me/operating-zones/{zoneId}", zoneId)
                .with(customer(otherSubject)))
            .andExpect(status().isNotFound());

        mockMvc.perform(delete("/v1/dashers/me/operating-zones/{zoneId}", zoneId)
                .with(customer(dasherSubject)))
            .andExpect(status().isNoContent());
    }

    @Test
    void validatesMotorVehicleAndZonePayloadsBeforeWriting() throws Exception {
        String subject = "invalid-dasher-" + UUID.randomUUID();

        mockMvc.perform(post("/v1/dashers/me/onboarding")
                .with(customer(subject))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"vehicleType": "CAR", "vehicleModel": "Camry"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Motor vehicles")));

        Long driverCount = jdbcClient.sql("SELECT count(*) FROM driver_profiles WHERE user_id = :userId")
            .param("userId", userId(subject))
            .query(Long.class)
            .single();
        assertThat(driverCount).isZero();
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
            .claim("name", "Integration Dasher")
            .claim("locale", "en-US"));
    }
}