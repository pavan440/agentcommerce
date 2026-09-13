-- Flyway Migration V4: Create Agent Governance, Memory & Safety Schema

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE agent_conversations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    agent_role VARCHAR(32) NOT NULL CHECK (agent_role IN ('CUSTOMER_AGENT', 'VENDOR_AGENT', 'DRIVER_AGENT', 'OPERATOR_AGENT')),
    title VARCHAR(255),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'PAUSED', 'ARCHIVED', 'TERMINATED')),
    retention_days INTEGER NOT NULL DEFAULT 90 CHECK (retention_days > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX agent_conversations_user_role_idx ON agent_conversations(user_id, agent_role, status);

CREATE TABLE agent_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES agent_conversations(id) ON DELETE CASCADE,
    sender_role VARCHAR(32) NOT NULL CHECK (sender_role IN ('USER', 'AGENT', 'SYSTEM', 'TOOL')),
    content TEXT NOT NULL,
    sanitized_content TEXT,
    prompt_injection_flag BOOLEAN NOT NULL DEFAULT FALSE,
    prompt_injection_score NUMERIC(4,3),
    tool_calls JSONB,
    tool_results JSONB,
    tokens_used INTEGER,
    latency_ms INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX agent_messages_conversation_idx ON agent_messages(conversation_id, created_at);

CREATE TABLE agent_memories (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL CHECK (role IN ('CUSTOMER', 'VENDOR', 'DRIVER')),
    memory_type VARCHAR(32) NOT NULL CHECK (memory_type IN ('PREFERENCE', 'DIETARY', 'BEHAVIOR', 'FACT', 'OPERATIONAL_RULE')),
    memory_key VARCHAR(120) NOT NULL,
    memory_value TEXT NOT NULL,
    embedding vector(1536),
    confidence_score NUMERIC(4,3) NOT NULL DEFAULT 1.000 CHECK (confidence_score >= 0.000 AND confidence_score <= 1.000),
    is_user_approved BOOLEAN NOT NULL DEFAULT TRUE,
    source_conversation_id UUID REFERENCES agent_conversations(id) ON DELETE SET NULL,
    consented_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX agent_memories_owner_role_idx ON agent_memories(owner_user_id, role) WHERE revoked_at IS NULL;

CREATE TABLE agent_approvals (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    conversation_id UUID REFERENCES agent_conversations(id) ON DELETE SET NULL,
    action_type VARCHAR(64) NOT NULL CHECK (action_type IN ('PLACE_ORDER', 'ACCEPT_SUBSTITUTION', 'REJECT_ORDER', 'BULK_PRICE_CHANGE', 'ACCEPT_DELIVERY_OFFER', 'CANCEL_ORDER', 'ISSUE_REFUND')),
    action_payload JSONB NOT NULL,
    estimated_financial_impact_minor BIGINT,
    currency CHAR(3) DEFAULT 'USD',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED')),
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    decided_at TIMESTAMPTZ,
    decision_reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT agent_approvals_currency_format CHECK (currency IS NULL OR currency ~ '^[A-Z]{3}$')
);

CREATE INDEX agent_approvals_user_status_idx ON agent_approvals(user_id, status);
CREATE INDEX agent_approvals_expiry_idx ON agent_approvals(expires_at) WHERE status = 'PENDING';

CREATE TABLE agent_policies (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    agent_role VARCHAR(32) NOT NULL CHECK (agent_role IN ('CUSTOMER_AGENT', 'VENDOR_AGENT', 'DRIVER_AGENT')),
    policy_type VARCHAR(64) NOT NULL CHECK (policy_type IN ('AUTO_ORDER', 'AUTO_ACCEPT_OFFER', 'AUTO_SUBSTITUTION', 'PRICE_CHANGE_LIMIT')),
    policy_rules JSONB NOT NULL DEFAULT '{}'::jsonb,
    max_auto_amount_minor BIGINT CHECK (max_auto_amount_minor IS NULL OR max_auto_amount_minor >= 0),
    is_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT agent_policies_unique_user_role_type UNIQUE (user_id, agent_role, policy_type)
);

CREATE TABLE agent_actions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    conversation_id UUID REFERENCES agent_conversations(id) ON DELETE SET NULL,
    approval_id UUID REFERENCES agent_approvals(id) ON DELETE SET NULL,
    tool_name VARCHAR(120) NOT NULL,
    input_hash VARCHAR(64) NOT NULL,
    sanitized_inputs JSONB,
    output_summary JSONB,
    status VARCHAR(32) NOT NULL CHECK (status IN ('SUCCESS', 'FAILED', 'BLOCKED_BY_POLICY', 'REQUIRES_APPROVAL')),
    policy_check_passed BOOLEAN NOT NULL DEFAULT TRUE,
    duration_ms INTEGER NOT NULL CHECK (duration_ms >= 0),
    error_message TEXT,
    correlation_id VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX agent_actions_user_tool_idx ON agent_actions(user_id, tool_name, created_at);
CREATE INDEX agent_actions_correlation_idx ON agent_actions(correlation_id);

CREATE TABLE agent_killswitches (
    id UUID PRIMARY KEY,
    target_type VARCHAR(32) NOT NULL CHECK (target_type IN ('GLOBAL', 'AGENT_ROLE', 'TOOL', 'TENANT', 'USER')),
    target_identifier VARCHAR(120) NOT NULL,
    is_disabled BOOLEAN NOT NULL DEFAULT TRUE,
    reason VARCHAR(255) NOT NULL,
    triggered_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ
);

CREATE INDEX agent_killswitches_active_idx ON agent_killswitches(target_type, target_identifier) WHERE is_disabled = TRUE AND resolved_at IS NULL;
