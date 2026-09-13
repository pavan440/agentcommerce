-- Flyway Migration V2: Create Vendor Schema (V3 Specifications + Community Code)

CREATE TABLE vendors (
    id UUID PRIMARY KEY,
    legal_name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    category VARCHAR(64) NOT NULL DEFAULT 'RESTAURANT',
    tags JSONB NOT NULL DEFAULT '[]'::jsonb,
    logo_url VARCHAR(512),
    banner_url VARCHAR(512),
    description TEXT,
    support_email VARCHAR(320),
    support_phone VARCHAR(32),
    default_currency CHAR(3) NOT NULL DEFAULT 'USD',
    fee_tier_id VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'CLOSED')),
    created_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT vendors_currency_format CHECK (default_currency ~ '^[A-Z]{3}$'),
    CONSTRAINT vendors_non_blank_names CHECK (
        length(trim(legal_name)) > 0 AND 
        length(trim(display_name)) > 0 AND 
        length(trim(slug)) > 0
    )
);

CREATE UNIQUE INDEX vendors_slug_idx ON vendors(lower(slug));
CREATE INDEX vendors_status_category_idx ON vendors(status, category);

CREATE TABLE vendor_memberships (
    vendor_id UUID NOT NULL REFERENCES vendors(id) ON DELETE RESTRICT,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    role VARCHAR(32) NOT NULL CHECK (role IN ('OWNER', 'ADMIN', 'MANAGER', 'STAFF')),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED')),
    invited_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    joined_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (vendor_id, user_id),
    CONSTRAINT vendor_memberships_joined_check CHECK (
        (status = 'ACTIVE' AND joined_at IS NOT NULL) OR (status != 'ACTIVE')
    )
);

CREATE INDEX vendor_memberships_user_status_idx ON vendor_memberships(user_id, status);
CREATE INDEX vendor_memberships_vendor_role_status_idx ON vendor_memberships(vendor_id, role, status);

CREATE TABLE vendor_invitations (
    id UUID PRIMARY KEY,
    vendor_id UUID NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
    email VARCHAR(320) NOT NULL,
    role VARCHAR(32) NOT NULL CHECK (role IN ('OWNER', 'ADMIN', 'MANAGER', 'STAFF')),
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'REVOKED')),
    invited_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    accepted_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX vendor_invitations_vendor_email_pending_idx ON vendor_invitations(vendor_id, lower(email)) WHERE status = 'PENDING';

CREATE TABLE vendor_locations (
    id UUID PRIMARY KEY,
    vendor_id UUID NOT NULL REFERENCES vendors(id) ON DELETE RESTRICT,
    community_code VARCHAR(32),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PENDING_VERIFICATION', 'ACTIVE', 'TEMPORARILY_CLOSED', 'SUSPENDED', 'CLOSED')),
    address_line_1 VARCHAR(255) NOT NULL,
    address_line_2 VARCHAR(255),
    locality VARCHAR(120) NOT NULL,
    administrative_area VARCHAR(120) NOT NULL,
    postal_code VARCHAR(32) NOT NULL,
    country_code CHAR(2) NOT NULL DEFAULT 'US',
    formatted_address VARCHAR(512) NOT NULL,
    coordinates GEOGRAPHY(POINT, 4326) NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    logo_url VARCHAR(512),
    banner_url VARCHAR(512),
    website_url VARCHAR(512),
    google_place_id VARCHAR(255),
    average_rating NUMERIC(3,2) NOT NULL DEFAULT 0.00 CHECK (average_rating >= 0.00 AND average_rating <= 5.00),
    review_count INTEGER NOT NULL DEFAULT 0 CHECK (review_count >= 0),
    contact_email VARCHAR(320),
    contact_phone VARCHAR(32),
    driver_pickup_instructions TEXT,
    customer_pickup_instructions TEXT,
    parking_instructions TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT vendor_locations_vendor_slug_unique UNIQUE (vendor_id, slug),
    CONSTRAINT vendor_locations_country_code_format CHECK (country_code ~ '^[A-Z]{2}$')
);

CREATE INDEX vendor_locations_coordinates_idx ON vendor_locations USING GIST(coordinates);
CREATE INDEX vendor_locations_vendor_status_idx ON vendor_locations(vendor_id, status);
CREATE INDEX vendor_locations_community_code_idx ON vendor_locations(community_code);

CREATE TABLE vendor_membership_locations (
    vendor_id UUID NOT NULL,
    user_id UUID NOT NULL,
    vendor_location_id UUID NOT NULL REFERENCES vendor_locations(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (vendor_id, user_id, vendor_location_id),
    FOREIGN KEY (vendor_id, user_id) REFERENCES vendor_memberships(vendor_id, user_id) ON DELETE CASCADE
);

CREATE TABLE vendor_location_hours (
    id UUID PRIMARY KEY,
    vendor_location_id UUID NOT NULL REFERENCES vendor_locations(id) ON DELETE CASCADE,
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    opens_at TIME NOT NULL,
    closes_at TIME NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT vendor_location_hours_open_close_diff CHECK (opens_at != closes_at),
    CONSTRAINT vendor_location_hours_unique UNIQUE (vendor_location_id, day_of_week, opens_at, closes_at)
);

CREATE TABLE vendor_location_special_hours (
    id UUID PRIMARY KEY,
    vendor_location_id UUID NOT NULL REFERENCES vendor_locations(id) ON DELETE CASCADE,
    local_date DATE NOT NULL,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    opens_at TIME,
    closes_at TIME,
    reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT vendor_location_special_hours_unique UNIQUE (vendor_location_id, local_date),
    CONSTRAINT vendor_location_special_hours_valid CHECK (
        (is_closed = TRUE AND opens_at IS NULL AND closes_at IS NULL) OR
        (is_closed = FALSE AND opens_at IS NOT NULL AND closes_at IS NOT NULL AND opens_at != closes_at)
    )
);

CREATE TABLE vendor_service_areas (
    id UUID PRIMARY KEY,
    vendor_location_id UUID NOT NULL REFERENCES vendor_locations(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    area GEOGRAPHY(MULTIPOLYGON, 4326) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    priority INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT vendor_service_areas_location_name_unique UNIQUE (vendor_location_id, name)
);

CREATE INDEX vendor_service_areas_area_idx ON vendor_service_areas USING GIST(area);

CREATE TABLE vendor_location_commerce_settings (
    vendor_location_id UUID PRIMARY KEY REFERENCES vendor_locations(id) ON DELETE CASCADE,
    currency CHAR(3) NOT NULL,
    is_accepting_orders BOOLEAN NOT NULL DEFAULT FALSE,
    auto_accept_orders BOOLEAN NOT NULL DEFAULT FALSE,
    paused_until TIMESTAMPTZ,
    pause_reason VARCHAR(255),
    busy_mode_prep_padding_minutes INTEGER NOT NULL DEFAULT 0 CHECK (busy_mode_prep_padding_minutes >= 0),
    is_delivery_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_pickup_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_curbside_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    order_notification_channel VARCHAR(32) NOT NULL DEFAULT 'TABLET' CHECK (order_notification_channel IN ('TABLET', 'POS', 'WEBHOOK', 'SMS', 'EMAIL')),
    default_preparation_minutes INTEGER NOT NULL CHECK (default_preparation_minutes > 0),
    minimum_preparation_minutes INTEGER NOT NULL CHECK (minimum_preparation_minutes > 0),
    maximum_preparation_minutes INTEGER NOT NULL CHECK (maximum_preparation_minutes > 0),
    minimum_order_amount_minor BIGINT NOT NULL DEFAULT 0 CHECK (minimum_order_amount_minor >= 0),
    packaging_fee_minor BIGINT NOT NULL DEFAULT 0 CHECK (packaging_fee_minor >= 0),
    tax_calculation_mode VARCHAR(32) NOT NULL DEFAULT 'PROVIDER',
    prices_include_tax BOOLEAN NOT NULL DEFAULT FALSE,
    default_product_tax_code VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT vendor_commerce_prep_range CHECK (
        minimum_preparation_minutes <= default_preparation_minutes AND
        default_preparation_minutes <= maximum_preparation_minutes
    ),
    CONSTRAINT vendor_commerce_currency_format CHECK (currency ~ '^[A-Z]{3}$')
);
