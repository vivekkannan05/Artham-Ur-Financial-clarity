-- ═══════════════════════════════════════════════════════════════
-- V6__intelligence.sql — Batch 6: Advisor, Calendar, Estate, Documents
--
-- Creates: recommendation, calendar_event, estate_info, document
-- SPEC REFS: FR-7.1–FR-7.7
-- ═══════════════════════════════════════════════════════════════

-- ─── Recommendation ──────────────────────────────────────────
-- Stores advisor engine output with full audit trail.
-- Users can accept/reject/defer each recommendation.
CREATE TABLE recommendation (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id        UUID            NOT NULL REFERENCES household(id) ON DELETE CASCADE,
    trigger_description TEXT            NOT NULL,
    rule_name           VARCHAR(100)    NOT NULL,
    severity            VARCHAR(20)     NOT NULL,
    category            VARCHAR(50)     NOT NULL,
    evidence_json       JSONB,
    action              TEXT            NOT NULL,
    expected_impact     TEXT,
    downside_risk       TEXT,
    ai_explanation      TEXT,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    user_decision       TEXT,
    decision_timestamp  TIMESTAMPTZ,
    spec_version        VARCHAR(20),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rec_household_status ON recommendation(household_id, status);

-- ─── Calendar Event ──────────────────────────────────────────
CREATE TABLE calendar_event (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id        UUID            NOT NULL REFERENCES household(id) ON DELETE CASCADE,
    event_type          VARCHAR(30)     NOT NULL,
    event_date          DATE            NOT NULL,
    recurrence          VARCHAR(20)     NOT NULL DEFAULT 'NONE',
    description         TEXT            NOT NULL,
    linked_entity_type  VARCHAR(50),
    linked_entity_id    UUID,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cal_household_date ON calendar_event(household_id, event_date);

-- ─── Estate Info ─────────────────────────────────────────────
-- Succession metadata per holding or account.
CREATE TABLE estate_info (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    holding_id              UUID REFERENCES holding(id),
    account_id              UUID REFERENCES account(id),
    nominee_name            VARCHAR(255),
    joint_holders_json      JSONB,
    beneficiary_designation VARCHAR(255),
    will_status             VARCHAR(20)     NOT NULL DEFAULT 'NO_WILL',
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_estate_holding ON estate_info(holding_id);
CREATE INDEX idx_estate_account ON estate_info(account_id);

-- ─── Document ────────────────────────────────────────────────
-- Polymorphic attachment: any entity type + entity ID.
CREATE TABLE document (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name           VARCHAR(255)    NOT NULL,
    file_path           TEXT            NOT NULL,
    document_type       VARCHAR(50),
    linked_entity_type  VARCHAR(50),
    linked_entity_id    UUID,
    document_date       DATE,
    sensitivity         VARCHAR(20)     NOT NULL DEFAULT 'NORMAL',
    source              VARCHAR(50),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_doc_entity ON document(linked_entity_type, linked_entity_id);
