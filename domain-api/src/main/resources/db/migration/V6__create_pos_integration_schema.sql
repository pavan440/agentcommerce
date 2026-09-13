-- Flyway Migration V6: Create POS Integration & In-Store Sales Intelligence Schema

CREATE TABLE pos_connections (
    id UUID PRIMARY KEY,
    vendor_location_id UUID NOT NULL REFERENCES vendor_locations(id) ON DELETE CASCADE,
    provider VARCHAR(64) NOT NULL CHECK (provider IN ('SQUARE', 'CLOVER', 'TOAST', 'LIGHTSPEED', 'DELIVERECT', 'OMNIVORE', 'CUSTOM_WEBHOOK', 'MANUAL_BRIDGE')),
    external_merchant_id VARCHAR(255),
    external_location_id VARCHAR(255),
    oauth_access_token_encrypted TEXT,
    oauth_refresh_token_encrypted TEXT,
    token_expires_at TIMESTAMPTZ,
    webhook_secret_encrypted TEXT,
    is_catalog_sync_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_inventory_sync_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_sales_sync_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_sync_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL DEFAULT 'DISCONNECTED' CHECK (status IN ('CONNECTED', 'PENDING_AUTH', 'ERROR', 'DISCONNECTED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pos_connections_location_provider_unique UNIQUE (vendor_location_id, provider)
);

CREATE INDEX pos_connections_loc_status_idx ON pos_connections(vendor_location_id, status);

CREATE TABLE in_store_sales_stream (
    id UUID PRIMARY KEY,
    vendor_location_id UUID NOT NULL REFERENCES vendor_locations(id) ON DELETE CASCADE,
    pos_connection_id UUID REFERENCES pos_connections(id) ON DELETE SET NULL,
    external_transaction_id VARCHAR(255),
    gross_amount_minor BIGINT NOT NULL CHECK (gross_amount_minor >= 0),
    net_amount_minor BIGINT NOT NULL CHECK (net_amount_minor >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'USD',
    item_count INTEGER NOT NULL DEFAULT 1 CHECK (item_count > 0),
    items_breakdown JSONB NOT NULL DEFAULT '[]'::jsonb,
    transaction_timestamp TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT in_store_sales_currency_format CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX in_store_sales_loc_time_idx ON in_store_sales_stream(vendor_location_id, transaction_timestamp);

CREATE TABLE inventory_sync_logs (
    id UUID PRIMARY KEY,
    vendor_location_id UUID NOT NULL REFERENCES vendor_locations(id) ON DELETE CASCADE,
    sku VARCHAR(120) NOT NULL,
    change_source VARCHAR(32) NOT NULL CHECK (change_source IN ('IN_STORE_POS_SALE', 'IN_STORE_RESTOCK', 'ONLINE_ORDER', 'MANUAL_OVERRIDE')),
    previous_quantity INTEGER NOT NULL,
    quantity_delta INTEGER NOT NULL,
    new_quantity INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX inventory_sync_logs_loc_sku_time_idx ON inventory_sync_logs(vendor_location_id, sku, created_at);

CREATE TABLE hourly_sales_baselines (
    id UUID PRIMARY KEY,
    vendor_location_id UUID NOT NULL REFERENCES vendor_locations(id) ON DELETE CASCADE,
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    hour_of_day SMALLINT NOT NULL CHECK (hour_of_day BETWEEN 0 AND 23),
    average_hourly_revenue_minor BIGINT NOT NULL DEFAULT 0 CHECK (average_hourly_revenue_minor >= 0),
    p25_revenue_minor BIGINT NOT NULL DEFAULT 0 CHECK (p25_revenue_minor >= 0),
    p75_revenue_minor BIGINT NOT NULL DEFAULT 0 CHECK (p75_revenue_minor >= 0),
    last_recalculated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT hourly_sales_baselines_unique UNIQUE (vendor_location_id, day_of_week, hour_of_day)
);

CREATE INDEX hourly_sales_baselines_lookup_idx ON hourly_sales_baselines(vendor_location_id, day_of_week, hour_of_day);
