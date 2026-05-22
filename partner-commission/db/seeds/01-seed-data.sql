-- 기본 테넌트
INSERT INTO tenants (id, name, status, attribution_window_minutes, attribution_strategy, confirmation_condition, confirmation_days, commission_rule_type, commission_rule_value, created_at)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', '테스트 쇼핑몰', 'ACTIVE', 43200, 'LAST_CLICK', 'TIME_BASED', 14, 'PERCENTAGE', 10.00, NOW())
ON CONFLICT (id) DO NOTHING;

-- 기본 API 키
INSERT INTO api_keys (id, tenant_id, api_key, status, created_at)
VALUES ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'sk_test_abc123', 'ACTIVE', NOW())
ON CONFLICT (id) DO NOTHING;

-- 테스트 파트너
INSERT INTO partners (id, name, email, status, created_at)
VALUES ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', '지수', 'jisoo@example.com', 'ACTIVE', NOW())
ON CONFLICT (id) DO NOTHING;

-- 파트너 소속 (멤버십)
INSERT INTO memberships (id, partner_id, tenant_id, tier_level, commission_rate, status, joined_at)
VALUES ('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'GOLD', 10.00, 'ACTIVE', NOW())
ON CONFLICT (id) DO NOTHING;

-- 추천 코드
INSERT INTO referral_codes (id, tenant_id, partner_id, code, created_at, expires_at)
VALUES ('00000000-0000-0000-0000-000000000040', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DEMO10', NOW(), NULL)
ON CONFLICT (id) DO NOTHING;