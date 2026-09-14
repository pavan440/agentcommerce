package com.agentcommerce.domain.identity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class UserProfileRepository {

    private final JdbcClient jdbcClient;

    UserProfileRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    Optional<CustomerProfileResponse> findProfile(UUID userId) {
        Optional<ProfileRow> profile = jdbcClient.sql("""
                SELECT u.id, u.email, u.phone, u.profile_image_url,
                       p.display_name, p.locale, p.timezone,
                       ARRAY(SELECT jsonb_array_elements_text(p.dietary_restrictions)) AS dietary_restrictions,
                       ARRAY(SELECT jsonb_array_elements_text(p.allergens)) AS allergens,
                       p.substitution_mode, p.auto_order_enabled,
                       p.auto_order_max_amount_minor, p.default_tip_percentage, p.version
                FROM users u
                JOIN customer_profiles p ON p.user_id = u.id
                WHERE u.id = :userId AND u.status = 'ACTIVE'
                """)
            .param("userId", userId)
            .query((resultSet, rowNumber) -> new ProfileRow(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("email"),
                resultSet.getString("phone"),
                resultSet.getString("profile_image_url"),
                resultSet.getString("display_name"),
                resultSet.getString("locale"),
                resultSet.getString("timezone"),
                readTextArray(resultSet, "dietary_restrictions"),
                readTextArray(resultSet, "allergens"),
                resultSet.getString("substitution_mode"),
                resultSet.getBoolean("auto_order_enabled"),
                resultSet.getLong("auto_order_max_amount_minor"),
                resultSet.getBigDecimal("default_tip_percentage"),
                resultSet.getLong("version")
            ))
            .optional();

        return profile.map(row -> new CustomerProfileResponse(
            row.id(),
            row.email(),
            row.phone(),
            row.profileImageUrl(),
            row.displayName(),
            row.locale(),
            row.timezone(),
            row.dietaryRestrictions(),
            row.allergens(),
            row.substitutionMode(),
            row.autoOrderEnabled(),
            row.autoOrderMaxAmountMinor(),
            row.defaultTipPercentage(),
            row.version(),
            roles(userId)
        ));
    }

    @Transactional
    Optional<CustomerProfileResponse> updateProfile(UUID userId, UpdateCustomerProfileRequest request) {
        int rows = jdbcClient.sql("""
                UPDATE customer_profiles
                SET display_name = COALESCE(:displayName, display_name),
                    locale = COALESCE(:locale, locale),
                    timezone = COALESCE(:timezone, timezone),
                    dietary_restrictions = CASE
                        WHEN :dietaryProvided THEN CAST(:dietaryRestrictions AS jsonb)
                        ELSE dietary_restrictions
                    END,
                    allergens = CASE
                        WHEN :allergensProvided THEN CAST(:allergens AS jsonb)
                        ELSE allergens
                    END,
                    substitution_mode = COALESCE(:substitutionMode, substitution_mode),
                    auto_order_enabled = COALESCE(:autoOrderEnabled, auto_order_enabled),
                    auto_order_max_amount_minor = COALESCE(:autoOrderMaxAmountMinor, auto_order_max_amount_minor),
                    default_tip_percentage = COALESCE(:defaultTipPercentage, default_tip_percentage),
                    updated_at = now(),
                    version = version + 1
                WHERE user_id = :userId AND version = :expectedVersion
                """)
            .param("userId", userId)
            .param("expectedVersion", request.expectedVersion())
            .param("displayName", request.displayName())
            .param("locale", request.locale())
            .param("timezone", request.timezone())
            .param("dietaryProvided", request.dietaryRestrictions() != null)
            .param("dietaryRestrictions", toJsonArray(request.dietaryRestrictions()))
            .param("allergensProvided", request.allergens() != null)
            .param("allergens", toJsonArray(request.allergens()))
            .param("substitutionMode", request.substitutionMode())
            .param("autoOrderEnabled", request.autoOrderEnabled())
            .param("autoOrderMaxAmountMinor", request.autoOrderMaxAmountMinor())
            .param("defaultTipPercentage", request.defaultTipPercentage())
            .update();

        if (rows == 0) {
            return Optional.empty();
        }

        jdbcClient.sql("""
                UPDATE users
                SET profile_image_url = COALESCE(:profileImageUrl, profile_image_url),
                    updated_at = now(),
                    version = version + 1
                WHERE id = :userId
                """)
            .param("userId", userId)
            .param("profileImageUrl", request.profileImageUrl())
            .update();

        return findProfile(userId);
    }

    List<CustomerAddressResponse> findAddresses(UUID userId) {
        return jdbcClient.sql("""
                SELECT id, label, address_line_1, address_line_2, building_name, gate_code,
                       locality, administrative_area, postal_code, country_code, formatted_address,
                       ST_Y(coordinates::geometry) AS latitude,
                       ST_X(coordinates::geometry) AS longitude,
                       delivery_instructions, is_default, created_at, updated_at
                FROM customer_addresses
                WHERE user_id = :userId
                ORDER BY is_default DESC, created_at ASC
                """)
            .param("userId", userId)
            .query(this::mapAddress)
            .list();
    }

    @Transactional
    CustomerAddressResponse createAddress(UUID userId, CreateCustomerAddressRequest request) {
        lockUser(userId);
        boolean makeDefault = request.isDefault() || jdbcClient.sql("""
                SELECT count(*) = 0
                FROM customer_addresses
                WHERE user_id = :userId
                """)
            .param("userId", userId)
            .query(Boolean.class)
            .single();

        if (makeDefault) {
            jdbcClient.sql("""
                    UPDATE customer_addresses
                    SET is_default = FALSE, updated_at = now()
                    WHERE user_id = :userId AND is_default = TRUE
                    """)
                .param("userId", userId)
                .update();
        }

        UUID addressId = UUID.randomUUID();
        jdbcClient.sql("""
                INSERT INTO customer_addresses (
                    id, user_id, label, address_line_1, address_line_2, building_name, gate_code,
                    locality, administrative_area, postal_code, country_code, formatted_address,
                    coordinates, delivery_instructions, is_default
                ) VALUES (
                    :id, :userId, :label, :addressLine1, :addressLine2, :buildingName, :gateCode,
                    :locality, :administrativeArea, :postalCode, :countryCode, :formattedAddress,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                    :deliveryInstructions, :isDefault
                )
                """)
            .param("id", addressId)
            .param("userId", userId)
            .param("label", request.label().trim().toUpperCase(Locale.ROOT))
            .param("addressLine1", request.addressLine1())
            .param("addressLine2", request.addressLine2())
            .param("buildingName", request.buildingName())
            .param("gateCode", request.gateCode())
            .param("locality", request.locality())
            .param("administrativeArea", request.administrativeArea())
            .param("postalCode", request.postalCode())
            .param("countryCode", request.countryCode() != null ? request.countryCode() : "US")
            .param("formattedAddress", request.formattedAddress())
            .param("longitude", request.longitude())
            .param("latitude", request.latitude())
            .param("deliveryInstructions", request.deliveryInstructions())
            .param("isDefault", makeDefault)
            .update();

        return findAddress(userId, addressId).orElseThrow();
    }

    @Transactional
    boolean deleteAddress(UUID userId, UUID addressId) {
        lockUser(userId);
        int rows = jdbcClient.sql("""
                DELETE FROM customer_addresses
                WHERE id = :addressId AND user_id = :userId
                """)
            .param("addressId", addressId)
            .param("userId", userId)
            .update();

        if (rows == 0) {
            return false;
        }

        jdbcClient.sql("""
                UPDATE customer_addresses
                SET is_default = TRUE, updated_at = now()
                WHERE id = (
                    SELECT id
                    FROM customer_addresses
                    WHERE user_id = :userId
                    ORDER BY created_at ASC
                    LIMIT 1
                )
                  AND NOT EXISTS (
                    SELECT 1
                    FROM customer_addresses
                    WHERE user_id = :userId AND is_default = TRUE
                )
                """)
            .param("userId", userId)
            .update();
        return true;
    }

    Optional<UUID> findDeviceTokenOwner(String token) {
        return jdbcClient.sql("""
                SELECT user_id
                FROM user_device_tokens
                WHERE token = :token
                """)
            .param("token", token)
            .query(UUID.class)
            .optional();
    }

    @Transactional
    DeviceTokenResponse registerDeviceToken(UUID userId, RegisterDeviceTokenRequest request) {
        Optional<UUID> existingId = jdbcClient.sql("""
                SELECT id
                FROM user_device_tokens
                WHERE user_id = :userId AND token = :token
                """)
            .param("userId", userId)
            .param("token", request.token())
            .query(UUID.class)
            .optional();

        UUID tokenId = existingId.orElseGet(UUID::randomUUID);
        if (existingId.isPresent()) {
            jdbcClient.sql("""
                    UPDATE user_device_tokens
                    SET platform = :platform, is_active = TRUE, last_used_at = now()
                    WHERE id = :id
                    """)
                .param("id", tokenId)
                .param("platform", request.platform())
                .update();
        } else {
            jdbcClient.sql("""
                    INSERT INTO user_device_tokens (id, user_id, token, platform)
                    VALUES (:id, :userId, :token, :platform)
                    """)
                .param("id", tokenId)
                .param("userId", userId)
                .param("token", request.token())
                .param("platform", request.platform())
                .update();
        }
        return findDeviceToken(userId, tokenId).orElseThrow();
    }

    List<DeviceTokenResponse> findDeviceTokens(UUID userId) {
        return jdbcClient.sql("""
                SELECT id, platform, is_active, last_used_at, created_at
                FROM user_device_tokens
                WHERE user_id = :userId
                ORDER BY created_at DESC
                """)
            .param("userId", userId)
            .query(this::mapDeviceToken)
            .list();
    }

    boolean deleteDeviceToken(UUID userId, UUID tokenId) {
        return jdbcClient.sql("""
                DELETE FROM user_device_tokens
                WHERE id = :tokenId AND user_id = :userId
                """)
            .param("tokenId", tokenId)
            .param("userId", userId)
            .update() > 0;
    }

    EmergencyContactResponse createEmergencyContact(UUID userId, CreateEmergencyContactRequest request) {
        UUID contactId = UUID.randomUUID();
        jdbcClient.sql("""
                INSERT INTO user_emergency_contacts (id, user_id, contact_name, relationship, phone)
                VALUES (:id, :userId, :contactName, :relationship, :phone)
                """)
            .param("id", contactId)
            .param("userId", userId)
            .param("contactName", request.contactName())
            .param("relationship", request.relationship())
            .param("phone", request.phone())
            .update();
        return findEmergencyContact(userId, contactId).orElseThrow();
    }

    List<EmergencyContactResponse> findEmergencyContacts(UUID userId) {
        return jdbcClient.sql("""
                SELECT id, contact_name, relationship, phone, created_at
                FROM user_emergency_contacts
                WHERE user_id = :userId
                ORDER BY created_at ASC
                """)
            .param("userId", userId)
            .query(this::mapEmergencyContact)
            .list();
    }

    boolean deleteEmergencyContact(UUID userId, UUID contactId) {
        return jdbcClient.sql("""
                DELETE FROM user_emergency_contacts
                WHERE id = :contactId AND user_id = :userId
                """)
            .param("contactId", contactId)
            .param("userId", userId)
            .update() > 0;
    }

    ConsentResponse recordConsent(
        UUID userId,
        String consentType,
        RecordConsentRequest request
    ) {
        UUID consentId = UUID.randomUUID();
        jdbcClient.sql("""
                INSERT INTO consents (
                    id, user_id, consent_type, policy_version, granted_at, revoked_at
                ) VALUES (
                    :id, :userId, :consentType, :policyVersion, now(),
                    CASE WHEN :granted THEN NULL ELSE now() END
                )
                """)
            .param("id", consentId)
            .param("userId", userId)
            .param("consentType", consentType)
            .param("policyVersion", request.policyVersion())
            .param("granted", request.granted())
            .update();
        return findConsent(userId, consentId).orElseThrow();
    }

    List<ConsentResponse> findCurrentConsents(UUID userId) {
        return jdbcClient.sql("""
                SELECT DISTINCT ON (consent_type)
                       id, consent_type, policy_version, granted_at, revoked_at, created_at
                FROM consents
                WHERE user_id = :userId
                ORDER BY consent_type, created_at DESC
                """)
            .param("userId", userId)
            .query(this::mapConsent)
            .list();
    }

    private Optional<CustomerAddressResponse> findAddress(UUID userId, UUID addressId) {
        return jdbcClient.sql("""
                SELECT id, label, address_line_1, address_line_2, building_name, gate_code,
                       locality, administrative_area, postal_code, country_code, formatted_address,
                       ST_Y(coordinates::geometry) AS latitude,
                       ST_X(coordinates::geometry) AS longitude,
                       delivery_instructions, is_default, created_at, updated_at
                FROM customer_addresses
                WHERE user_id = :userId AND id = :addressId
                """)
            .param("userId", userId)
            .param("addressId", addressId)
            .query(this::mapAddress)
            .optional();
    }

    private Optional<DeviceTokenResponse> findDeviceToken(UUID userId, UUID tokenId) {
        return jdbcClient.sql("""
                SELECT id, platform, is_active, last_used_at, created_at
                FROM user_device_tokens
                WHERE user_id = :userId AND id = :tokenId
                """)
            .param("userId", userId)
            .param("tokenId", tokenId)
            .query(this::mapDeviceToken)
            .optional();
    }

    private Optional<EmergencyContactResponse> findEmergencyContact(UUID userId, UUID contactId) {
        return jdbcClient.sql("""
                SELECT id, contact_name, relationship, phone, created_at
                FROM user_emergency_contacts
                WHERE user_id = :userId AND id = :contactId
                """)
            .param("userId", userId)
            .param("contactId", contactId)
            .query(this::mapEmergencyContact)
            .optional();
    }

    private Optional<ConsentResponse> findConsent(UUID userId, UUID consentId) {
        return jdbcClient.sql("""
                SELECT id, consent_type, policy_version, granted_at, revoked_at, created_at
                FROM consents
                WHERE user_id = :userId AND id = :consentId
                """)
            .param("userId", userId)
            .param("consentId", consentId)
            .query(this::mapConsent)
            .optional();
    }

    private Set<String> roles(UUID userId) {
        return Set.copyOf(new LinkedHashSet<>(jdbcClient.sql("""
                SELECT role
                FROM user_roles
                WHERE user_id = :userId
                ORDER BY role
                """)
            .param("userId", userId)
            .query(String.class)
            .list()));
    }

    private void lockUser(UUID userId) {
        jdbcClient.sql("SELECT id FROM users WHERE id = :userId FOR UPDATE")
            .param("userId", userId)
            .query(UUID.class)
            .single();
    }

    private CustomerAddressResponse mapAddress(ResultSet resultSet, int rowNumber) throws SQLException {
        return new CustomerAddressResponse(
            resultSet.getObject("id", UUID.class),
            resultSet.getString("label"),
            resultSet.getString("address_line_1"),
            resultSet.getString("address_line_2"),
            resultSet.getString("building_name"),
            resultSet.getString("gate_code"),
            resultSet.getString("locality"),
            resultSet.getString("administrative_area"),
            resultSet.getString("postal_code"),
            resultSet.getString("country_code"),
            resultSet.getString("formatted_address"),
            resultSet.getDouble("latitude"),
            resultSet.getDouble("longitude"),
            resultSet.getString("delivery_instructions"),
            resultSet.getBoolean("is_default"),
            toInstant(resultSet.getTimestamp("created_at")),
            toInstant(resultSet.getTimestamp("updated_at"))
        );
    }

    private DeviceTokenResponse mapDeviceToken(ResultSet resultSet, int rowNumber) throws SQLException {
        return new DeviceTokenResponse(
            resultSet.getObject("id", UUID.class),
            resultSet.getString("platform"),
            resultSet.getBoolean("is_active"),
            toInstant(resultSet.getTimestamp("last_used_at")),
            toInstant(resultSet.getTimestamp("created_at"))
        );
    }

    private EmergencyContactResponse mapEmergencyContact(ResultSet resultSet, int rowNumber) throws SQLException {
        return new EmergencyContactResponse(
            resultSet.getObject("id", UUID.class),
            resultSet.getString("contact_name"),
            resultSet.getString("relationship"),
            resultSet.getString("phone"),
            toInstant(resultSet.getTimestamp("created_at"))
        );
    }

    private ConsentResponse mapConsent(ResultSet resultSet, int rowNumber) throws SQLException {
        Timestamp revokedAt = resultSet.getTimestamp("revoked_at");
        return new ConsentResponse(
            resultSet.getObject("id", UUID.class),
            resultSet.getString("consent_type"),
            resultSet.getString("policy_version"),
            revokedAt == null,
            toInstant(resultSet.getTimestamp("granted_at")),
            toInstant(revokedAt),
            toInstant(resultSet.getTimestamp("created_at"))
        );
    }

    private List<String> readTextArray(ResultSet resultSet, String column) throws SQLException {
        java.sql.Array values = resultSet.getArray(column);
        if (values == null) {
            return List.of();
        }
        return Arrays.asList((String[]) values.getArray());
    }

    private String toJsonArray(List<String> values) {
        if (values == null) {
            return "[]";
        }
        return values.stream()
            .map(this::toJsonString)
            .collect(Collectors.joining(",", "[", "]"));
    }

    private String toJsonString(String value) {
        String escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\f", "\\f")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }

    private record ProfileRow(
        UUID id,
        String email,
        String phone,
        String profileImageUrl,
        String displayName,
        String locale,
        String timezone,
        List<String> dietaryRestrictions,
        List<String> allergens,
        String substitutionMode,
        boolean autoOrderEnabled,
        long autoOrderMaxAmountMinor,
        java.math.BigDecimal defaultTipPercentage,
        long version
    ) {
    }
}
