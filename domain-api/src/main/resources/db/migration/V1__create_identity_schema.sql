-- Flyway Migration V1: Create Identity & User Schema

CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320),
    phone VARCHAR(32),
    profile_image_url VARCHAR(512),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE user_identities (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    issuer VARCHAR(512) NOT NULL,
    subject VARCHAR(512) NOT NULL,
    provider VARCHAR(255) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT user_identities_issuer_subject_unique UNIQUE (issuer, subject)
);

CREATE INDEX user_identities_user_id_idx ON user_identities(user_id);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL CHECK (role IN ('CUSTOMER', 'VENDOR_MEMBER', 'DRIVER', 'OPERATOR')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role)
);

CREATE TABLE customer_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    display_name VARCHAR(255) NOT NULL,
    locale VARCHAR(32) NOT NULL DEFAULT 'en-US',
    timezone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    dietary_restrictions JSONB NOT NULL DEFAULT '[]'::jsonb,
    allergens JSONB NOT NULL DEFAULT '[]'::jsonb,
    substitution_mode VARCHAR(32) NOT NULL DEFAULT 'APPROVAL_REQUIRED' CHECK (substitution_mode IN ('PREFERENCE_BASED', 'APPROVAL_REQUIRED')),
    auto_order_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    auto_order_max_amount_minor BIGINT DEFAULT 0 CHECK (auto_order_max_amount_minor >= 0),
    default_tip_percentage NUMERIC(4,2) NOT NULL DEFAULT 15.00 CHECK (default_tip_percentage >= 0.00),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE customer_addresses (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    label VARCHAR(64) NOT NULL DEFAULT 'HOME',
    address_line_1 VARCHAR(255) NOT NULL,
    address_line_2 VARCHAR(255),
    building_name VARCHAR(120),
    gate_code VARCHAR(32),
    locality VARCHAR(120) NOT NULL,
    administrative_area VARCHAR(120) NOT NULL,
    postal_code VARCHAR(32) NOT NULL,
    country_code CHAR(2) NOT NULL DEFAULT 'US',
    formatted_address VARCHAR(512) NOT NULL,
    coordinates GEOGRAPHY(POINT, 4326) NOT NULL,
    delivery_instructions TEXT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT customer_addresses_country_code_format CHECK (country_code ~ '^[A-Z]{2}$')
);

CREATE INDEX customer_addresses_user_id_idx ON customer_addresses(user_id);
CREATE INDEX customer_addresses_coordinates_idx ON customer_addresses USING GIST(coordinates);

CREATE TABLE driver_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_VERIFICATION' CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED')),
    vehicle_type VARCHAR(32) NOT NULL DEFAULT 'CAR' CHECK (vehicle_type IN ('CAR', 'BICYCLE', 'SCOOTER', 'FOOT')),
    vehicle_make VARCHAR(64),
    vehicle_model VARCHAR(64),
    vehicle_color VARCHAR(32),
    license_plate VARCHAR(32),
    license_number VARCHAR(64),
    background_check_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' CHECK (background_check_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    max_active_pickups INTEGER NOT NULL DEFAULT 3 CHECK (max_active_pickups > 0),
    average_rating NUMERIC(3,2) NOT NULL DEFAULT 5.00 CHECK (average_rating >= 0.00 AND average_rating <= 5.00),
    total_deliveries INTEGER NOT NULL DEFAULT 0 CHECK (total_deliveries >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE driver_operating_zones (
    id UUID PRIMARY KEY,
    driver_user_id UUID NOT NULL REFERENCES driver_profiles(user_id) ON DELETE CASCADE,
    zone_name VARCHAR(120) NOT NULL,
    area GEOGRAPHY(MULTIPOLYGON, 4326) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX driver_operating_zones_area_idx ON driver_operating_zones USING GIST(area);

CREATE TABLE user_device_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(512) NOT NULL UNIQUE,
    platform VARCHAR(32) NOT NULL CHECK (platform IN ('IOS', 'ANDROID', 'WEB')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_used_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX user_device_tokens_user_idx ON user_device_tokens(user_id);

CREATE TABLE user_emergency_contacts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    contact_name VARCHAR(255) NOT NULL,
    relationship VARCHAR(64),
    phone VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE consents (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    consent_type VARCHAR(64) NOT NULL CHECK (consent_type IN ('LOCATION_TRACKING', 'AGENT_PERSONALIZATION')),
    policy_version VARCHAR(64) NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT consents_revocation_after_grant CHECK (revoked_at IS NULL OR revoked_at >= granted_at)
);

CREATE INDEX consents_user_id_type_idx ON consents(user_id, consent_type);
