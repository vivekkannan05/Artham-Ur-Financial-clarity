-- ═══════════════════════════════════════════════════════════════
-- V3__goals.sql — Batch 3: Goals, Glide-Path, Allocation
-- SPEC REFS: FR-4.1 through FR-4.6
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE goal (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id         UUID            NOT NULL REFERENCES household(id) ON DELETE CASCADE,
    name                 VARCHAR(255)    NOT NULL,
    goal_type            VARCHAR(30)     NOT NULL,
    beneficiary_id       UUID            REFERENCES person(id),
    owner_id             UUID            REFERENCES person(id),
    target_date          DATE,
    target_amount_today  NUMERIC(15,2),
    inflation_rate       NUMERIC(10,6),
    expected_return_rate NUMERIC(10,6),
    priority             VARCHAR(20)     NOT NULL,
    status               VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    sip_step_up_pct      NUMERIC(10,6),
    created_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_goal_household ON goal(household_id);

CREATE TABLE glide_path (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id         UUID            NOT NULL UNIQUE REFERENCES goal(id) ON DELETE CASCADE,
    glide_path_type VARCHAR(20)     NOT NULL,
    schedule_json   JSONB           NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE goal_allocation (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id               UUID            NOT NULL REFERENCES goal(id) ON DELETE CASCADE,
    holding_id            UUID            NOT NULL REFERENCES holding(id),
    allocation_percentage NUMERIC(5,2)    NOT NULL,
    created_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE(goal_id, holding_id)
);

CREATE INDEX idx_goal_alloc_goal ON goal_allocation(goal_id);
CREATE INDEX idx_goal_alloc_holding ON goal_allocation(holding_id);
