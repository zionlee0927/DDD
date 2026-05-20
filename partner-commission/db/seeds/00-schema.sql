-- 테이블 생성
CREATE TABLE IF NOT EXISTS tenants (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    status VARCHAR(255),
    attribution_window_minutes BIGINT NOT NULL DEFAULT 43200,
    attribution_strategy VARCHAR(255) DEFAULT 'LAST_CLICK',
    confirmation_condition VARCHAR(255) DEFAULT 'TIME_BASED',
    confirmation_days INT NOT NULL DEFAULT 14,
    webhook_url VARCHAR(255),
    webhook_secret VARCHAR(255),
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS api_keys (
    id UUID PRIMARY KEY,
    tenant_id UUID,
    api_key VARCHAR(255),
    status VARCHAR(255),
    created_at TIMESTAMP,
    expires_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS partners (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255),
    status VARCHAR(255),
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS memberships (
    id UUID PRIMARY KEY,
    partner_id UUID,
    tenant_id UUID,
    tier_level VARCHAR(255),
    commission_rate NUMERIC(38,2),
    status VARCHAR(255),
    joined_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tracking_links (
    id UUID PRIMARY KEY,
    tenant_id UUID,
    partner_id UUID,
    target_url VARCHAR(255),
    tracking_code VARCHAR(255) UNIQUE,
    created_at TIMESTAMP,
    expires_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS clicks (
    id UUID PRIMARY KEY,
    tenant_id UUID,
    tracking_code VARCHAR(255),
    ip_address VARCHAR(255),
    user_agent VARCHAR(255),
    clicked_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS referral_codes (
    id UUID PRIMARY KEY,
    tenant_id UUID,
    partner_id UUID,
    code VARCHAR(255) UNIQUE,
    created_at TIMESTAMP,
    expires_at TIMESTAMP
);
