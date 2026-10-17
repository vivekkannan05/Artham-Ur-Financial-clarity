-- ═══════════════════════════════════════════════════════════════
-- V5__advanced_modules.sql — Batch 5: Tax, Insurance, Property, Scenarios
--
-- Creates: insurance_policy, property
-- Alters: liability (add FK to property now that table exists)
-- SPEC REFS: FR-6.1 (tax), FR-6.2 (insurance), FR-6.3 (property)
-- ═══════════════════════════════════════════════════════════════

-- ─── Property ────────────────────────────────────────────────
-- Must be created BEFORE the liability FK constraint since
-- liability.property_id references this table.
CREATE TABLE property (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id          UUID            NOT NULL REFERENCES household(id) ON DELETE CASCADE,
    name                  VARCHAR(255)    NOT NULL,
    address               TEXT,
    purchase_date         DATE,
    purchase_cost         NUMERIC(15,2),
    current_value         NUMERIC(15,2)   NOT NULL,
    valuation_date        DATE,
    monthly_rental_income NUMERIC(15,2)   DEFAULT 0,
    monthly_expenses      NUMERIC(15,2)   DEFAULT 0,
    is_self_occupied      BOOLEAN         DEFAULT TRUE,
    notes                 TEXT,
    created_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_property_household ON property(household_id);

-- Now add the FK constraint from liability.property_id → property(id)
-- The column already exists (created in V4) but had no REFERENCES constraint
-- because the property table didn't exist yet.
ALTER TABLE liability
    ADD CONSTRAINT fk_liability_property
    FOREIGN KEY (property_id) REFERENCES property(id);

-- ─── Insurance Policy ────────────────────────────────────────
-- Tracks all insurance policies for coverage gap analysis (FR-6.2).
CREATE TABLE insurance_policy (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id      UUID            NOT NULL REFERENCES household(id) ON DELETE CASCADE,
    policy_type       VARCHAR(30)     NOT NULL,
    insured_person_id UUID            NOT NULL REFERENCES person(id),
    owner_id          UUID            NOT NULL REFERENCES person(id),
    policy_name       VARCHAR(255),
    insurer           VARCHAR(255)    NOT NULL,
    policy_number     VARCHAR(100),
    sum_assured       NUMERIC(15,2)   NOT NULL,
    annual_premium    NUMERIC(15,2)   NOT NULL,
    premium_due_date  DATE,
    start_date        DATE            NOT NULL,
    end_date          DATE,
    renewal_status    VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    notes             TEXT,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_insurance_household ON insurance_policy(household_id);
CREATE INDEX idx_insurance_insured ON insurance_policy(insured_person_id);
CREATE INDEX idx_insurance_type ON insurance_policy(policy_type);
