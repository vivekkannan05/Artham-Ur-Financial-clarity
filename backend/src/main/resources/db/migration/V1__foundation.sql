-- ═══════════════════════════════════════════════════════════════
-- V1__foundation.sql — Batch 1: Foundation Tables
--
-- Creates: household, person, income, expense
-- SPEC REFS: FR-1.1, FR-1.2, FR-2.1, FR-2.2, AD-2
--
-- FLYWAY NAMING CONVENTION:
--   V<version>__<description>.sql
--   - V1, V2, V3... — applied in order, once, never re-run
--   - Double underscore __ separates version from description
--   - Flyway tracks which versions have been applied in a
--     'flyway_schema_history' table
--
-- Spring Boot runs this automatically on startup.
-- ═══════════════════════════════════════════════════════════════

-- ─── Household ───────────────────────────────────────────────
-- Top-level entity. All financial data belongs to a household.
-- Per AD-2: every table has id (UUID PK), created_at, updated_at.
CREATE TABLE household (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255)    NOT NULL,
    description TEXT,
    status      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ─── Person ──────────────────────────────────────────────────
-- A member of a household (primary user, spouse, child, etc.).
-- pan_encrypted stores AES-256 ciphertext, NOT plaintext PAN.
CREATE TABLE person (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id      UUID            NOT NULL REFERENCES household(id) ON DELETE CASCADE,
    name              VARCHAR(255)    NOT NULL,
    relationship      VARCHAR(30)     NOT NULL,
    date_of_birth     DATE,
    gender            VARCHAR(10),
    pan_encrypted     TEXT,
    tax_regime        VARCHAR(10),
    residency_status  VARCHAR(50),
    citizenship       VARCHAR(50),
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Index: look up all members of a household (used by HouseholdService)
CREATE INDEX idx_person_household ON person(household_id);

-- ─── Income ──────────────────────────────────────────────────
-- A single income source for a person.
-- amount: BigDecimal with 2-decimal precision (per NFR-1.2).
CREATE TABLE income (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    person_id   UUID            NOT NULL REFERENCES person(id) ON DELETE CASCADE,
    income_type VARCHAR(30)     NOT NULL,
    amount      NUMERIC(15,2)   NOT NULL,
    frequency   VARCHAR(20)     NOT NULL,
    start_date  DATE            NOT NULL,
    end_date    DATE,
    source      VARCHAR(255),
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Index: look up all incomes for a person
CREATE INDEX idx_income_person ON income(person_id);

-- ─── Expense ─────────────────────────────────────────────────
-- A spending entry for a person.
-- category: CONSUMPTION or INVESTMENT_OUTFLOW (critical for cash-flow).
CREATE TABLE expense (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    person_id   UUID            NOT NULL REFERENCES person(id) ON DELETE CASCADE,
    name        VARCHAR(255)    NOT NULL,
    amount      NUMERIC(15,2)   NOT NULL,
    frequency   VARCHAR(20)     NOT NULL,
    category    VARCHAR(30)     NOT NULL,
    start_date  DATE            NOT NULL,
    end_date    DATE,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Index: look up all expenses for a person
CREATE INDEX idx_expense_person ON expense(person_id);
