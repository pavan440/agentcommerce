package com.agentcommerce.domain.vendor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class VendorRepository {

    private final JdbcClient jdbcClient;

    public VendorRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Transactional
    public VendorResponse createVendor(UUID userId, CreateVendorRequest request) {
        UUID vendorId = UUID.randomUUID();
        String tagsJson = toJsonArray(request.tags());

        jdbcClient.sql("""
                INSERT INTO vendors (
                    id, legal_name, display_name, slug, category, tags,
                    logo_url, banner_url, description, support_email, support_phone,
                    default_currency, fee_tier_id, status, created_by_user_id
                ) VALUES (
                    :id, :legalName, :displayName, :slug, :category, CAST(:tags AS jsonb),
                    :logoUrl, :bannerUrl, :description, :supportEmail, :supportPhone,
                    :defaultCurrency, :feeTierId, 'DRAFT', :userId
                )
                """)
            .param("id", vendorId)
            .param("legalName", request.legalName())
            .param("displayName", request.displayName())
            .param("slug", request.slug().trim().toLowerCase(Locale.ROOT))
            .param("category", request.category() != null ? request.category().trim().toUpperCase(Locale.ROOT) : "RESTAURANT")
            .param("tags", tagsJson)
            .param("logoUrl", request.logoUrl())
            .param("bannerUrl", request.bannerUrl())
            .param("description", request.description())
            .param("supportEmail", request.supportEmail())
            .param("supportPhone", request.supportPhone())
            .param("defaultCurrency", request.defaultCurrency() != null ? request.defaultCurrency() : "USD")
            .param("feeTierId", request.feeTierId())
            .param("userId", userId)
            .update();

        jdbcClient.sql("""
                INSERT INTO user_roles (user_id, role)
                VALUES (:userId, 'VENDOR_MEMBER')
                ON CONFLICT DO NOTHING
                """)
            .param("userId", userId)
            .update();

        jdbcClient.sql("""
                INSERT INTO vendor_memberships (vendor_id, user_id, role, status, joined_at)
                VALUES (:vendorId, :userId, 'OWNER', 'ACTIVE', now())
                """)
            .param("vendorId", vendorId)
            .param("userId", userId)
            .update();

        return findVendorById(vendorId).orElseThrow();
    }

    public Optional<VendorResponse> findVendorById(UUID vendorId) {
        return jdbcClient.sql("""
                SELECT id, legal_name, display_name, slug, category,
                       ARRAY(SELECT jsonb_array_elements_text(tags)) AS tags,
                       logo_url, banner_url, description, support_email, support_phone,
                       default_currency, fee_tier_id, status, created_by_user_id,
                       created_at, updated_at, version
                FROM vendors
                WHERE id = :id
                """)
            .param("id", vendorId)
            .query(this::mapVendorRow)
            .optional();
    }

    public List<VendorResponse> findVendorsByUserId(UUID userId) {
        return jdbcClient.sql("""
                SELECT v.id, v.legal_name, v.display_name, v.slug, v.category,
                       ARRAY(SELECT jsonb_array_elements_text(v.tags)) AS tags,
                       v.logo_url, v.banner_url, v.description, v.support_email, v.support_phone,
                       v.default_currency, v.fee_tier_id, v.status, v.created_by_user_id,
                       v.created_at, v.updated_at, v.version
                FROM vendors v
                JOIN vendor_memberships m ON m.vendor_id = v.id
                WHERE m.user_id = :userId AND m.status = 'ACTIVE'
                ORDER BY v.created_at DESC
                """)
            .param("userId", userId)
            .query(this::mapVendorRow)
            .list();
    }

    public boolean canCreateLocations(UUID userId, UUID vendorId) {
        return jdbcClient.sql("""
                SELECT EXISTS (
                    SELECT 1
                    FROM vendor_memberships
                    WHERE user_id = :userId
                      AND vendor_id = :vendorId
                      AND status = 'ACTIVE'
                      AND role IN ('OWNER', 'ADMIN')
                )
                """)
            .param("userId", userId)
            .param("vendorId", vendorId)
            .query(Boolean.class)
            .single();
    }

    public boolean canManageLocation(UUID userId, UUID locationId) {
        return jdbcClient.sql("""
                SELECT EXISTS (
                    SELECT 1
                    FROM vendor_memberships membership
                    JOIN vendor_locations location ON location.vendor_id = membership.vendor_id
                    WHERE membership.user_id = :userId
                      AND location.id = :locationId
                      AND membership.status = 'ACTIVE'
                      AND (
                          membership.role IN ('OWNER', 'ADMIN')
                          OR (
                              membership.role = 'MANAGER'
                              AND EXISTS (
                                  SELECT 1
                                  FROM vendor_membership_locations scope
                                  WHERE scope.vendor_id = membership.vendor_id
                                    AND scope.user_id = membership.user_id
                                    AND scope.vendor_location_id = location.id
                              )
                          )
                      )
                )
                """)
            .param("userId", userId)
            .param("locationId", locationId)
            .query(Boolean.class)
            .single();
    }
    @Transactional
    public VendorLocationResponse createVendorLocation(UUID vendorId, CreateVendorLocationRequest req) {
        UUID locationId = UUID.randomUUID();

        jdbcClient.sql("""
                INSERT INTO vendor_locations (
                    id, vendor_id, community_code, name, slug, status,
                    address_line_1, address_line_2, locality, administrative_area, postal_code, country_code,
                    formatted_address, coordinates, timezone, logo_url, banner_url, website_url, google_place_id,
                    contact_email, contact_phone, driver_pickup_instructions, customer_pickup_instructions, parking_instructions
                ) VALUES (
                    :id, :vendorId, :communityCode, :name, :slug, 'DRAFT',
                    :addressLine1, :addressLine2, :locality, :administrativeArea, :postalCode, :countryCode,
                    :formattedAddress, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :timezone,
                    :logoUrl, :bannerUrl, :websiteUrl, :googlePlaceId,
                    :contactEmail, :contactPhone, :driverPickupInstructions, :customerPickupInstructions, :parkingInstructions
                )
                """)
            .param("id", locationId)
            .param("vendorId", vendorId)
            .param("communityCode", req.communityCode().trim().toUpperCase(Locale.ROOT))
            .param("name", req.name())
            .param("slug", req.slug().trim().toLowerCase(Locale.ROOT))
            .param("addressLine1", req.addressLine1())
            .param("addressLine2", req.addressLine2())
            .param("locality", req.locality())
            .param("administrativeArea", req.administrativeArea())
            .param("postalCode", req.postalCode())
            .param("countryCode", req.countryCode() != null ? req.countryCode() : "US")
            .param("formattedAddress", req.formattedAddress())
            .param("longitude", req.longitude())
            .param("latitude", req.latitude())
            .param("timezone", req.timezone())
            .param("logoUrl", req.logoUrl())
            .param("bannerUrl", req.bannerUrl())
            .param("websiteUrl", req.websiteUrl())
            .param("googlePlaceId", req.googlePlaceId())
            .param("contactEmail", req.contactEmail())
            .param("contactPhone", req.contactPhone())
            .param("driverPickupInstructions", req.driverPickupInstructions())
            .param("customerPickupInstructions", req.customerPickupInstructions())
            .param("parkingInstructions", req.parkingInstructions())
            .update();

        jdbcClient.sql("""
                INSERT INTO vendor_location_commerce_settings (
                    vendor_location_id, currency, is_accepting_orders, auto_accept_orders,
                    default_preparation_minutes, minimum_preparation_minutes, maximum_preparation_minutes,
                    minimum_order_amount_minor, packaging_fee_minor, tax_calculation_mode
                ) VALUES (
                    :locationId, 'USD', TRUE, FALSE, 20, 10, 60, 0, 0, 'PROVIDER'
                )
                """)
            .param("locationId", locationId)
            .update();

        return findLocationById(locationId).orElseThrow();
    }

    public Optional<VendorLocationResponse> findLocationById(UUID locationId) {
        return jdbcClient.sql("""
                SELECT id, vendor_id, community_code, name, slug, status,
                       address_line_1, address_line_2, locality, administrative_area, postal_code, country_code,
                       formatted_address, ST_Y(coordinates::geometry) AS latitude, ST_X(coordinates::geometry) AS longitude,
                       timezone, logo_url, banner_url, website_url, google_place_id,
                       average_rating, review_count, contact_email, contact_phone,
                       driver_pickup_instructions, customer_pickup_instructions, parking_instructions,
                       created_at, updated_at, version
                FROM vendor_locations
                WHERE id = :id
                """)
            .param("id", locationId)
            .query(this::mapLocationRow)
            .optional();
    }

    public List<VendorLocationResponse> findLocationsByCommunity(String communityCode) {
        return jdbcClient.sql("""
                SELECT id, vendor_id, community_code, name, slug, status,
                       address_line_1, address_line_2, locality, administrative_area, postal_code, country_code,
                       formatted_address, ST_Y(coordinates::geometry) AS latitude, ST_X(coordinates::geometry) AS longitude,
                       timezone, logo_url, banner_url, website_url, google_place_id,
                       average_rating, review_count, contact_email, contact_phone,
                       driver_pickup_instructions, customer_pickup_instructions, parking_instructions,
                       created_at, updated_at, version
                FROM vendor_locations
                WHERE community_code = :communityCode AND status = 'ACTIVE'
                ORDER BY name ASC
                """)
            .param("communityCode", communityCode.trim().toUpperCase(Locale.ROOT))
            .query(this::mapLocationRow)
            .list();
    }

    public Optional<VendorCommerceSettingsResponse> findCommerceSettings(UUID locationId) {
        return jdbcClient.sql("""
                SELECT vendor_location_id, currency, is_accepting_orders, auto_accept_orders,
                       paused_until, pause_reason, busy_mode_prep_padding_minutes,
                       is_delivery_enabled, is_pickup_enabled, is_curbside_enabled,
                       order_notification_channel, default_preparation_minutes,
                       minimum_preparation_minutes, maximum_preparation_minutes,
                       minimum_order_amount_minor, packaging_fee_minor, tax_calculation_mode,
                       prices_include_tax, default_product_tax_code, version
                FROM vendor_location_commerce_settings
                WHERE vendor_location_id = :id
                """)
            .param("id", locationId)
            .query((rs, rowNum) -> new VendorCommerceSettingsResponse(
                rs.getObject("vendor_location_id", UUID.class),
                rs.getString("currency"),
                rs.getBoolean("is_accepting_orders"),
                rs.getBoolean("auto_accept_orders"),
                toInstant(rs.getTimestamp("paused_until")),
                rs.getString("pause_reason"),
                rs.getInt("busy_mode_prep_padding_minutes"),
                rs.getBoolean("is_delivery_enabled"),
                rs.getBoolean("is_pickup_enabled"),
                rs.getBoolean("is_curbside_enabled"),
                rs.getString("order_notification_channel"),
                rs.getInt("default_preparation_minutes"),
                rs.getInt("minimum_preparation_minutes"),
                rs.getInt("maximum_preparation_minutes"),
                rs.getLong("minimum_order_amount_minor"),
                rs.getLong("packaging_fee_minor"),
                rs.getString("tax_calculation_mode"),
                rs.getBoolean("prices_include_tax"),
                rs.getString("default_product_tax_code"),
                rs.getLong("version")
            ))
            .optional();
    }

    @Transactional
    public boolean updateCommerceSettings(UUID locationId, UpdateCommerceSettingsRequest req) {
        int rows = jdbcClient.sql("""
                UPDATE vendor_location_commerce_settings
                SET
                    is_accepting_orders = COALESCE(:isAcceptingOrders, is_accepting_orders),
                    auto_accept_orders = COALESCE(:autoAcceptOrders, auto_accept_orders),
                    paused_until = :pausedUntil,
                    pause_reason = :pauseReason,
                    busy_mode_prep_padding_minutes = COALESCE(:busyModePadding, busy_mode_prep_padding_minutes),
                    is_delivery_enabled = COALESCE(:isDeliveryEnabled, is_delivery_enabled),
                    is_pickup_enabled = COALESCE(:isPickupEnabled, is_pickup_enabled),
                    is_curbside_enabled = COALESCE(:isCurbsideEnabled, is_curbside_enabled),
                    order_notification_channel = COALESCE(:orderChannel, order_notification_channel),
                    default_preparation_minutes = COALESCE(:defaultPrep, default_preparation_minutes),
                    minimum_preparation_minutes = COALESCE(:minPrep, minimum_preparation_minutes),
                    maximum_preparation_minutes = COALESCE(:maxPrep, maximum_preparation_minutes),
                    minimum_order_amount_minor = COALESCE(:minOrderMinor, minimum_order_amount_minor),
                    packaging_fee_minor = COALESCE(:packagingFeeMinor, packaging_fee_minor),
                    tax_calculation_mode = COALESCE(:taxMode, tax_calculation_mode),
                    prices_include_tax = COALESCE(:pricesIncludeTax, prices_include_tax),
                    default_product_tax_code = COALESCE(:defaultTaxCode, default_product_tax_code),
                    updated_at = now(),
                    version = version + 1
                WHERE vendor_location_id = :locationId AND version = :expectedVersion
                """)
            .param("locationId", locationId)
            .param("expectedVersion", req.expectedVersion())
            .param("isAcceptingOrders", req.isAcceptingOrders())
            .param("autoAcceptOrders", req.autoAcceptOrders())
            .param("pausedUntil", req.pausedUntil() != null ? Timestamp.from(req.pausedUntil()) : null)
            .param("pauseReason", req.pauseReason())
            .param("busyModePadding", req.busyModePrepPaddingMinutes())
            .param("isDeliveryEnabled", req.isDeliveryEnabled())
            .param("isPickupEnabled", req.isPickupEnabled())
            .param("isCurbsideEnabled", req.isCurbsideEnabled())
            .param("orderChannel", req.orderNotificationChannel())
            .param("defaultPrep", req.defaultPreparationMinutes())
            .param("minPrep", req.minimumPreparationMinutes())
            .param("maxPrep", req.maximumPreparationMinutes())
            .param("minOrderMinor", req.minimumOrderAmountMinor())
            .param("packagingFeeMinor", req.packagingFeeMinor())
            .param("taxMode", req.taxCalculationMode())
            .param("pricesIncludeTax", req.pricesIncludeTax())
            .param("defaultTaxCode", req.defaultProductTaxCode())
            .update();

        return rows > 0;
    }

    private String toJsonArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        return values.stream()
            .map(this::toJsonString)
            .collect(java.util.stream.Collectors.joining(",", "[", "]"));
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

    private List<String> readTags(ResultSet resultSet) throws SQLException {
        java.sql.Array tags = resultSet.getArray("tags");
        if (tags == null) {
            return List.of();
        }
        return Arrays.asList((String[]) tags.getArray());
    }

    private VendorResponse mapVendorRow(ResultSet rs, int rowNum) throws SQLException {
        List<String> tagsList = readTags(rs);

        return new VendorResponse(
            rs.getObject("id", UUID.class),
            rs.getString("legal_name"),
            rs.getString("display_name"),
            rs.getString("slug"),
            rs.getString("category"),
            tagsList,
            rs.getString("logo_url"),
            rs.getString("banner_url"),
            rs.getString("description"),
            rs.getString("support_email"),
            rs.getString("support_phone"),
            rs.getString("default_currency"),
            rs.getString("fee_tier_id"),
            rs.getString("status"),
            rs.getObject("created_by_user_id", UUID.class),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at")),
            rs.getLong("version")
        );
    }

    private VendorLocationResponse mapLocationRow(ResultSet rs, int rowNum) throws SQLException {
        return new VendorLocationResponse(
            rs.getObject("id", UUID.class),
            rs.getObject("vendor_id", UUID.class),
            rs.getString("community_code"),
            rs.getString("name"),
            rs.getString("slug"),
            rs.getString("status"),
            rs.getString("address_line_1"),
            rs.getString("address_line_2"),
            rs.getString("locality"),
            rs.getString("administrative_area"),
            rs.getString("postal_code"),
            rs.getString("country_code"),
            rs.getString("formatted_address"),
            rs.getDouble("latitude"),
            rs.getDouble("longitude"),
            rs.getString("timezone"),
            rs.getString("logo_url"),
            rs.getString("banner_url"),
            rs.getString("website_url"),
            rs.getString("google_place_id"),
            rs.getDouble("average_rating"),
            rs.getInt("review_count"),
            rs.getString("contact_email"),
            rs.getString("contact_phone"),
            rs.getString("driver_pickup_instructions"),
            rs.getString("customer_pickup_instructions"),
            rs.getString("parking_instructions"),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at")),
            rs.getLong("version")
        );
    }

    private Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }
}
