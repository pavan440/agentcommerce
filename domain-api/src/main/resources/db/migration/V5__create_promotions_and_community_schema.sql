-- Flyway Migration V5: Create Hyperlocal Community Promotions & Sales Strategy Schema

CREATE TABLE community_zones (
    id UUID PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,
    city VARCHAR(120) NOT NULL,
    boundary GEOGRAPHY(MULTIPOLYGON, 4326) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT community_zones_code_format CHECK (code ~ '^[A-Z0-9_-]{3,32}$')
);

CREATE INDEX community_zones_code_idx ON community_zones(code);
CREATE INDEX community_zones_boundary_idx ON community_zones USING GIST(boundary);

CREATE TABLE vendor_sales_targets (
    id UUID PRIMARY KEY,
    vendor_location_id UUID NOT NULL REFERENCES vendor_locations(id) ON DELETE CASCADE,
    target_date DATE NOT NULL,
    target_revenue_minor BIGINT NOT NULL CHECK (target_revenue_minor > 0),
    current_actual_revenue_minor BIGINT NOT NULL DEFAULT 0 CHECK (current_actual_revenue_minor >= 0),
    projected_deficit_minor BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS', 'ACHIEVED', 'MISSED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT vendor_sales_targets_unique_loc_date UNIQUE (vendor_location_id, target_date)
);

CREATE INDEX vendor_sales_targets_loc_date_idx ON vendor_sales_targets(vendor_location_id, target_date);

CREATE TABLE promotions (
    id UUID PRIMARY KEY,
    vendor_location_id UUID NOT NULL REFERENCES vendor_locations(id) ON DELETE CASCADE,
    community_code VARCHAR(32) REFERENCES community_zones(code) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    strategy_type VARCHAR(64) NOT NULL DEFAULT 'SLOW_PERIOD_BOOST' CHECK (strategy_type IN ('SLOW_PERIOD_BOOST', 'EXCESS_INVENTORY', 'NEW_CUSTOMER_ACQUISITION', 'HAPPY_HOUR')),
    discount_type VARCHAR(32) NOT NULL DEFAULT 'PERCENTAGE' CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT', 'FREE_ITEM', 'BOGO')),
    discount_value NUMERIC(6,2) NOT NULL CHECK (discount_value > 0),
    minimum_order_minor BIGINT NOT NULL DEFAULT 0 CHECK (minimum_order_minor >= 0),
    is_online_delivery_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_in_store_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    in_store_qr_code VARCHAR(128),
    target_radius_meters INTEGER NOT NULL DEFAULT 3000 CHECK (target_radius_meters > 0),
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    max_redemptions INTEGER CHECK (max_redemptions IS NULL OR max_redemptions > 0),
    total_redemptions INTEGER NOT NULL DEFAULT 0 CHECK (total_redemptions >= 0),
    status VARCHAR(32) NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED', 'ACTIVE', 'EXPIRED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT promotions_time_window_valid CHECK (ends_at > starts_at)
);

CREATE INDEX promotions_loc_status_time_idx ON promotions(vendor_location_id, status, starts_at, ends_at);
CREATE INDEX promotions_community_code_idx ON promotions(community_code);

CREATE TABLE promotion_redemptions (
    id UUID PRIMARY KEY,
    promotion_id UUID NOT NULL REFERENCES promotions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    channel VARCHAR(32) NOT NULL CHECK (channel IN ('ONLINE_DELIVERY', 'IN_STORE_QR')),
    order_id UUID,
    discount_amount_minor BIGINT NOT NULL CHECK (discount_amount_minor >= 0),
    gross_order_amount_minor BIGINT NOT NULL CHECK (gross_order_amount_minor >= 0),
    redeemed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX promotion_redemptions_promo_idx ON promotion_redemptions(promotion_id);
CREATE INDEX promotion_redemptions_user_idx ON promotion_redemptions(user_id);

CREATE TABLE promotion_strategy_analytics (
    promotion_id UUID PRIMARY KEY REFERENCES promotions(id) ON DELETE CASCADE,
    baseline_sales_minor BIGINT NOT NULL CHECK (baseline_sales_minor >= 0),
    actual_promo_sales_minor BIGINT NOT NULL DEFAULT 0 CHECK (actual_promo_sales_minor >= 0),
    incremental_lift_minor BIGINT NOT NULL DEFAULT 0,
    total_discount_cost_minor BIGINT NOT NULL DEFAULT 0 CHECK (total_discount_cost_minor >= 0),
    net_incremental_profit_minor BIGINT NOT NULL DEFAULT 0,
    new_customers_acquired INTEGER NOT NULL DEFAULT 0 CHECK (new_customers_acquired >= 0),
    daily_goal_achieved BOOLEAN NOT NULL DEFAULT FALSE,
    strategy_rating VARCHAR(32) CHECK (strategy_rating IN ('HIGH_PERFORMING', 'MODERATE', 'UNDERPERFORMING')),
    calculated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE community_announcements (
    id UUID PRIMARY KEY,
    vendor_location_id UUID NOT NULL REFERENCES vendor_locations(id) ON DELETE CASCADE,
    community_code VARCHAR(32) REFERENCES community_zones(code) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    image_url VARCHAR(512),
    channel VARCHAR(32) NOT NULL DEFAULT 'FEED' CHECK (channel IN ('FEED', 'PUSH_NOTIFICATION', 'ALL')),
    target_radius_meters INTEGER NOT NULL DEFAULT 3000 CHECK (target_radius_meters > 0),
    published_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ
);

CREATE INDEX community_announcements_loc_idx ON community_announcements(vendor_location_id, published_at);
CREATE INDEX community_announcements_code_idx ON community_announcements(community_code);
