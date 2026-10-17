-- ═══════════════════════════════════════════════════════════════
-- V4__liabilities_networth.sql — Batch 4: Liabilities & Net Worth
--
-- Creates: liability, net_worth_snapshot
-- SPEC REFS: FR-5.1 (liabilities), FR-5.3 (net-worth history)
-- ═══════════════════════════════════════════════════════════════

-- ─── Liability ───────────────────────────────────────────────
-- Models all debt obligations: home loans, car loans, credit cards, etc.
-- The property_id column is a forward reference — the property table
-- comes in Batch 5 (FR-6.3). Until then, it stays NULL.
-- We do NOT add a REFERENCES constraint yet (target table doesn't exist).
CREATE TABLE liability (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id           UUID            NOT NULL REFERENCES household(id) ON DELETE CASCADE,
    liability_type         VARCHAR(30)     NOT NULL,
    status                 VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    lender                 VARCHAR(255)    NOT NULL,
    principal_outstanding  NUMERIC(15,2)   NOT NULL,
    original_principal     NUMERIC(15,2),
    interest_rate          NUMERIC(8,4)    NOT NULL,
    emi_amount             NUMERIC(15,2)   NOT NULL,
    tenure_start           DATE            NOT NULL,
    tenure_end             DATE,
    prepayment_total       NUMERIC(15,2)   DEFAULT 0,
    property_id            UUID,
    notes                  TEXT,
    created_at             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Index: look up all liabilities for a household (net-worth + cash-flow queries)
CREATE INDEX idx_liability_household ON liability(household_id);

-- Index: filter by status (we usually only want ACTIVE liabilities)
CREATE INDEX idx_liability_status ON liability(household_id, status);

-- ─── Net Worth Snapshot ──────────────────────────────────────
-- Append-only time-series table. Each row captures the household's
-- financial position at a point in time. Past snapshots are NEVER updated.
-- SPEC REF: FR-5.3 — "History is append-only; past snapshots are never modified."
CREATE TABLE net_worth_snapshot (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id             UUID            NOT NULL REFERENCES household(id) ON DELETE CASCADE,
    snapshot_date            DATE            NOT NULL,
    total_assets             NUMERIC(15,2)   NOT NULL,
    total_liabilities        NUMERIC(15,2)   NOT NULL,
    net_worth                NUMERIC(15,2)   NOT NULL,
    assets_by_class_json     JSONB,
    month_over_month_change  NUMERIC(15,2),
    year_over_year_change    NUMERIC(15,2),
    created_at               TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Composite index: time-series queries for net-worth chart
-- "Show me net worth for household X from Jan to Dec"
CREATE INDEX idx_nw_household_date ON net_worth_snapshot(household_id, snapshot_date);

-- Unique constraint: one snapshot per household per date
-- Prevents duplicate snapshots if the job runs twice on the same day
CREATE UNIQUE INDEX idx_nw_unique_household_date ON net_worth_snapshot(household_id, snapshot_date);
