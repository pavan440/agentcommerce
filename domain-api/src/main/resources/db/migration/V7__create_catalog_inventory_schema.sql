-- Flyway Migration V7: Catalog, Inventory, and Staged CSV Imports

CREATE TABLE catalog_items (
    id UUID PRIMARY KEY,
    vendor_location_id UUID NOT NULL REFERENCES vendor_locations(id) ON DELETE CASCADE,
    sku VARCHAR(120) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(120),
    price_minor BIGINT NOT NULL CHECK (price_minor >= 0),
    currency CHAR(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (vendor_location_id, sku)
);

CREATE TABLE inventory_records (
    id UUID PRIMARY KEY,
    catalog_item_id UUID NOT NULL UNIQUE REFERENCES catalog_items(id) ON DELETE CASCADE,
    vendor_location_id UUID NOT NULL REFERENCES vendor_locations(id) ON DELETE CASCADE,
    tracking_mode VARCHAR(24) NOT NULL DEFAULT 'QUANTITY' CHECK (tracking_mode IN ('QUANTITY','AVAILABILITY_ONLY')),
    quantity_on_hand INTEGER NOT NULL DEFAULT 0 CHECK (quantity_on_hand >= 0),
    quantity_reserved INTEGER NOT NULL DEFAULT 0 CHECK (quantity_reserved >= 0 AND quantity_reserved <= quantity_on_hand),
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    reorder_threshold INTEGER NOT NULL DEFAULT 0 CHECK (reorder_threshold >= 0),
    source VARCHAR(24) NOT NULL DEFAULT 'MANUAL' CHECK (source IN ('MANUAL','CSV','POS','AGENT')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX inventory_records_location_idx ON inventory_records(vendor_location_id, updated_at DESC);

CREATE TABLE inventory_imports (
    id UUID PRIMARY KEY,
    vendor_location_id UUID NOT NULL REFERENCES vendor_locations(id) ON DELETE CASCADE,
    submitted_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    file_name VARCHAR(255) NOT NULL,
    content_hash CHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('STAGED','COMMITTED','ROLLED_BACK')),
    total_rows INTEGER NOT NULL DEFAULT 0,
    valid_rows INTEGER NOT NULL DEFAULT 0,
    invalid_rows INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    committed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE inventory_import_rows (
    id UUID PRIMARY KEY,
    inventory_import_id UUID NOT NULL REFERENCES inventory_imports(id) ON DELETE CASCADE,
    row_number INTEGER NOT NULL,
    sku VARCHAR(120),
    name VARCHAR(255),
    category VARCHAR(120),
    price_minor BIGINT,
    currency CHAR(3),
    quantity_on_hand INTEGER,
    is_available BOOLEAN,
    reorder_threshold INTEGER,
    is_valid BOOLEAN NOT NULL,
    included BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,
    committed_item_id UUID REFERENCES catalog_items(id) ON DELETE SET NULL,
    UNIQUE (inventory_import_id, row_number)
);