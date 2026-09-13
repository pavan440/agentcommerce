-- Flyway Migration V3: Create Merchant Legal & Financial Schema

CREATE TABLE merchants (
    id UUID PRIMARY KEY,
    legal_entity_name VARCHAR(255) NOT NULL,
    dba_name VARCHAR(255) NOT NULL,
    business_type VARCHAR(64) NOT NULL DEFAULT 'LLC' CHECK (business_type IN ('LLC', 'CORPORATION', 'SOLE_PROPRIETORSHIP', 'PARTNERSHIP', 'INDIVIDUAL')),
    tax_id_last_four VARCHAR(4),
    country_code CHAR(2) NOT NULL DEFAULT 'US',
    support_email VARCHAR(320) NOT NULL,
    support_phone VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PENDING_VERIFICATION', 'VERIFIED', 'SUSPENDED', 'CLOSED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT merchants_country_code_format CHECK (country_code ~ '^[A-Z]{2}$')
);

CREATE INDEX merchants_status_idx ON merchants(status);

CREATE TABLE merchant_stripe_accounts (
    merchant_id UUID PRIMARY KEY REFERENCES merchants(id) ON DELETE CASCADE,
    stripe_connect_account_id VARCHAR(255) NOT NULL UNIQUE,
    payouts_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    charges_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    details_submitted BOOLEAN NOT NULL DEFAULT FALSE,
    default_currency CHAR(3) NOT NULL DEFAULT 'USD',
    onboarding_status VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED' CHECK (onboarding_status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'ACTION_REQUIRED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT merchant_stripe_currency_format CHECK (default_currency ~ '^[A-Z]{3}$')
);

CREATE TABLE merchant_fee_agreements (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    commission_rate_bps INTEGER NOT NULL DEFAULT 1500 CHECK (commission_rate_bps >= 0 AND commission_rate_bps <= 10000),
    flat_fee_minor BIGINT NOT NULL DEFAULT 30 CHECK (flat_fee_minor >= 0),
    effective_from TIMESTAMPTZ NOT NULL DEFAULT now(),
    effective_to TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUPERSEDED', 'PENDING')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT merchant_fee_effective_range CHECK (effective_to IS NULL OR effective_to > effective_from)
);

CREATE INDEX merchant_fee_agreements_merchant_status_idx ON merchant_fee_agreements(merchant_id, status);

CREATE TABLE merchant_payout_schedules (
    merchant_id UUID PRIMARY KEY REFERENCES merchants(id) ON DELETE CASCADE,
    interval_type VARCHAR(32) NOT NULL DEFAULT 'DAILY' CHECK (interval_type IN ('DAILY', 'WEEKLY', 'MONTHLY', 'MANUAL')),
    weekly_anchor_day SMALLINT CHECK (weekly_anchor_day BETWEEN 1 AND 7),
    monthly_anchor_day SMALLINT CHECK (monthly_anchor_day BETWEEN 1 AND 31),
    delay_days INTEGER NOT NULL DEFAULT 2 CHECK (delay_days >= 0),
    minimum_payout_minor BIGINT NOT NULL DEFAULT 1000 CHECK (minimum_payout_minor >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE merchant_bank_accounts (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    bank_name VARCHAR(120) NOT NULL,
    routing_number_last_four VARCHAR(4) NOT NULL,
    account_number_last_four VARCHAR(4) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'USD',
    is_default BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT merchant_bank_currency_format CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX merchant_bank_accounts_merchant_idx ON merchant_bank_accounts(merchant_id);
