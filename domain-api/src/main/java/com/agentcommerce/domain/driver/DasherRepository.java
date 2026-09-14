package com.agentcommerce.domain.driver;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class DasherRepository {

    private final JdbcClient jdbcClient;

    DasherRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    Optional<DasherProfileResponse> findProfile(UUID userId) {
        return jdbcClient.sql("""
                SELECT user_id, status, vehicle_type, vehicle_make, vehicle_model,
                       vehicle_color, license_plate, license_number, background_check_status,
                       is_online, max_active_pickups, average_rating, total_deliveries,
                       version, created_at, updated_at
                FROM driver_profiles
                WHERE user_id = :userId
                """)
            .param("userId", userId)
            .query(this::mapProfile)
            .optional();
    }

    @Transactional
    DasherProfileResponse createProfile(UUID userId, CreateDasherProfileRequest request) {
        jdbcClient.sql("""
                INSERT INTO driver_profiles (
                    user_id, vehicle_type, vehicle_make, vehicle_model, vehicle_color,
                    license_plate, license_number, max_active_pickups
                ) VALUES (
                    :userId, :vehicleType, :vehicleMake, :vehicleModel, :vehicleColor,
                    :licensePlate, :licenseNumber, :maxActivePickups
                )
                """)
            .param("userId", userId)
            .param("vehicleType", request.vehicleType())
            .param("vehicleMake", request.vehicleMake())
            .param("vehicleModel", request.vehicleModel())
            .param("vehicleColor", request.vehicleColor())
            .param("licensePlate", request.licensePlate())
            .param("licenseNumber", request.licenseNumber())
            .param("maxActivePickups", request.maxActivePickups() != null ? request.maxActivePickups() : 3)
            .update();

        jdbcClient.sql("""
                INSERT INTO user_roles (user_id, role)
                VALUES (:userId, 'DRIVER')
                ON CONFLICT (user_id, role) DO NOTHING
                """)
            .param("userId", userId)
            .update();

        return findProfile(userId).orElseThrow();
    }

    @Transactional
    Optional<DasherProfileResponse> updateProfile(
        UUID userId,
        UpdateDasherProfileRequest request
    ) {
        boolean clearMotorVehicle = "BICYCLE".equals(request.vehicleType())
            || "FOOT".equals(request.vehicleType());
        int rows = jdbcClient.sql("""
                UPDATE driver_profiles
                SET vehicle_type = COALESCE(:vehicleType, vehicle_type),
                    vehicle_make = CASE WHEN :clearMotorVehicle THEN NULL ELSE COALESCE(:vehicleMake, vehicle_make) END,
                    vehicle_model = CASE WHEN :clearMotorVehicle THEN NULL ELSE COALESCE(:vehicleModel, vehicle_model) END,
                    vehicle_color = CASE WHEN :clearMotorVehicle THEN NULL ELSE COALESCE(:vehicleColor, vehicle_color) END,
                    license_plate = CASE WHEN :clearMotorVehicle THEN NULL ELSE COALESCE(:licensePlate, license_plate) END,
                    license_number = CASE WHEN :clearMotorVehicle THEN NULL ELSE COALESCE(:licenseNumber, license_number) END,
                    max_active_pickups = COALESCE(:maxActivePickups, max_active_pickups),
                    updated_at = now(),
                    version = version + 1
                WHERE user_id = :userId AND version = :expectedVersion
                """)
            .param("userId", userId)
            .param("expectedVersion", request.expectedVersion())
            .param("vehicleType", request.vehicleType())
            .param("clearMotorVehicle", clearMotorVehicle)
            .param("vehicleMake", request.vehicleMake())
            .param("vehicleModel", request.vehicleModel())
            .param("vehicleColor", request.vehicleColor())
            .param("licensePlate", request.licensePlate())
            .param("licenseNumber", request.licenseNumber())
            .param("maxActivePickups", request.maxActivePickups())
            .update();
        return rows == 0 ? Optional.empty() : findProfile(userId);
    }

    @Transactional
    Optional<DasherProfileResponse> updateAvailability(
        UUID userId,
        UpdateDasherAvailabilityRequest request
    ) {
        int rows = jdbcClient.sql("""
                UPDATE driver_profiles
                SET is_online = :online,
                    updated_at = now(),
                    version = version + 1
                WHERE user_id = :userId AND version = :expectedVersion
                """)
            .param("userId", userId)
            .param("expectedVersion", request.expectedVersion())
            .param("online", request.online())
            .update();
        return rows == 0 ? Optional.empty() : findProfile(userId);
    }

    @Transactional
    Optional<DasherProfileResponse> reviewProfile(UUID userId, ReviewDasherRequest request) {
        int rows = jdbcClient.sql("""
                UPDATE driver_profiles
                SET status = :status,
                    background_check_status = :backgroundCheckStatus,
                    is_online = CASE WHEN :status = 'ACTIVE' THEN is_online ELSE FALSE END,
                    updated_at = now(),
                    version = version + 1
                WHERE user_id = :userId AND version = :expectedVersion
                """)
            .param("userId", userId)
            .param("expectedVersion", request.expectedVersion())
            .param("status", request.status())
            .param("backgroundCheckStatus", request.backgroundCheckStatus())
            .update();
        return rows == 0 ? Optional.empty() : findProfile(userId);
    }

    boolean hasRole(UUID userId, String role) {
        return jdbcClient.sql("""
                SELECT EXISTS (
                    SELECT 1 FROM user_roles WHERE user_id = :userId AND role = :role
                )
                """)
            .param("userId", userId)
            .param("role", role)
            .query(Boolean.class)
            .single();
    }

    boolean hasOperatingZone(UUID userId) {
        return countOperatingZones(userId) > 0;
    }

    int countOperatingZones(UUID userId) {
        return jdbcClient.sql("""
                SELECT count(*) FROM driver_operating_zones
                WHERE driver_user_id = :userId
                """)
            .param("userId", userId)
            .query(Integer.class)
            .single();
    }

    DasherOperatingZoneResponse createOperatingZone(
        UUID userId,
        String zoneName,
        String areaWkt
    ) {
        UUID zoneId = UUID.randomUUID();
        jdbcClient.sql("""
                INSERT INTO driver_operating_zones (id, driver_user_id, zone_name, area)
                VALUES (
                    :id, :userId, :zoneName,
                    ST_Multi(ST_GeomFromText(:areaWkt, 4326))::geography
                )
                """)
            .param("id", zoneId)
            .param("userId", userId)
            .param("zoneName", zoneName)
            .param("areaWkt", areaWkt)
            .update();
        return findOperatingZone(userId, zoneId).orElseThrow();
    }

    List<DasherOperatingZoneResponse> findOperatingZones(UUID userId) {
        return jdbcClient.sql("""
                SELECT id, zone_name, ST_AsGeoJSON(area::geometry) AS area_geo_json, created_at
                FROM driver_operating_zones
                WHERE driver_user_id = :userId
                ORDER BY created_at ASC
                """)
            .param("userId", userId)
            .query(this::mapOperatingZone)
            .list();
    }

    boolean deleteOperatingZone(UUID userId, UUID zoneId) {
        return jdbcClient.sql("""
                DELETE FROM driver_operating_zones
                WHERE id = :zoneId AND driver_user_id = :userId
                """)
            .param("zoneId", zoneId)
            .param("userId", userId)
            .update() > 0;
    }

    private Optional<DasherOperatingZoneResponse> findOperatingZone(UUID userId, UUID zoneId) {
        return jdbcClient.sql("""
                SELECT id, zone_name, ST_AsGeoJSON(area::geometry) AS area_geo_json, created_at
                FROM driver_operating_zones
                WHERE id = :zoneId AND driver_user_id = :userId
                """)
            .param("zoneId", zoneId)
            .param("userId", userId)
            .query(this::mapOperatingZone)
            .optional();
    }

    private DasherProfileResponse mapProfile(ResultSet resultSet, int rowNumber) throws SQLException {
        return new DasherProfileResponse(
            resultSet.getObject("user_id", UUID.class),
            resultSet.getString("status"),
            resultSet.getString("vehicle_type"),
            resultSet.getString("vehicle_make"),
            resultSet.getString("vehicle_model"),
            resultSet.getString("vehicle_color"),
            resultSet.getString("license_plate"),
            lastFour(resultSet.getString("license_number")),
            resultSet.getString("background_check_status"),
            resultSet.getBoolean("is_online"),
            resultSet.getInt("max_active_pickups"),
            resultSet.getBigDecimal("average_rating"),
            resultSet.getInt("total_deliveries"),
            resultSet.getLong("version"),
            toInstant(resultSet.getTimestamp("created_at")),
            toInstant(resultSet.getTimestamp("updated_at"))
        );
    }

    private DasherOperatingZoneResponse mapOperatingZone(ResultSet resultSet, int rowNumber) throws SQLException {
        return new DasherOperatingZoneResponse(
            resultSet.getObject("id", UUID.class),
            resultSet.getString("zone_name"),
            resultSet.getString("area_geo_json"),
            toInstant(resultSet.getTimestamp("created_at"))
        );
    }

    private String lastFour(String value) {
        if (value == null) {
            return null;
        }
        return value.substring(Math.max(0, value.length() - 4));
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }
}