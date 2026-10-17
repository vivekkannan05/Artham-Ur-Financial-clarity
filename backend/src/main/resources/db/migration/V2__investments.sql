-- ═══════════════════════════════════════════════════════════════
-- V2__investments.sql — Batch 2: Investment Tables + Instrument Catalog
--
-- Creates: account, instrument_type, holding, transaction, transaction_correction
-- Seeds: 23 instrument types from SPEC Section 14.1
-- SPEC REFS: FR-3.1 through FR-3.7, AD-3, Review R-1
-- ═══════════════════════════════════════════════════════════════

-- ─── Account ─────────────────────────────────────────────────
CREATE TABLE account (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id              UUID            NOT NULL REFERENCES person(id) ON DELETE CASCADE,
    institution_name      VARCHAR(255)    NOT NULL,
    account_number_masked VARCHAR(50),
    account_type          VARCHAR(30)     NOT NULL,
    currency              VARCHAR(3)      NOT NULL DEFAULT 'INR',
    status                VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_account_owner ON account(owner_id);

-- ─── Instrument Type (Reference Table) ──────────────────────
-- Pluggable catalog: new types added via INSERT, no code changes.
-- JSONB columns hold variable metadata per instrument type.
CREATE TABLE instrument_type (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                 VARCHAR(30)     NOT NULL UNIQUE,
    display_name         VARCHAR(100)    NOT NULL,
    asset_class          VARCHAR(30)     NOT NULL,
    tax_treatment_json   JSONB,
    valuation_method     VARCHAR(50),
    maturity_rules_json  JSONB,
    liquidity_profile    VARCHAR(20),
    required_fields_json JSONB,
    optional_fields_json JSONB,
    created_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_instrument_type_code ON instrument_type(code);

-- ─── Holding ─────────────────────────────────────────────────
CREATE TABLE holding (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                     VARCHAR(255)    NOT NULL,
    account_id               UUID            NOT NULL REFERENCES account(id),
    owner_id                 UUID            NOT NULL REFERENCES person(id),
    instrument_type_id       UUID            NOT NULL REFERENCES instrument_type(id),
    units                    NUMERIC(15,4),
    acquisition_cost_total   NUMERIC(15,2),
    acquisition_cost_per_unit NUMERIC(15,4),
    acquisition_date         DATE,
    current_value            NUMERIC(15,2),
    valuation_date           DATE,
    currency                 VARCHAR(3)      NOT NULL DEFAULT 'INR',
    status                   VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    provenance_source        VARCHAR(30),
    provenance_date          DATE,
    provenance_confidence    VARCHAR(20),
    created_at               TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_holding_account ON holding(account_id);
CREATE INDEX idx_holding_owner ON holding(owner_id);
CREATE INDEX idx_holding_instrument ON holding(instrument_type_id);

-- ─── Transaction (IMMUTABLE — per FR-3.4, Review R-1) ───────
-- In production: REVOKE UPDATE, DELETE ON transaction FROM artha_app;
CREATE TABLE transaction (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    holding_id        UUID            NOT NULL REFERENCES holding(id),
    owner_id          UUID            NOT NULL REFERENCES person(id),
    account_id        UUID            NOT NULL REFERENCES account(id),
    transaction_date  DATE            NOT NULL,
    amount            NUMERIC(15,2)   NOT NULL,
    transaction_type  VARCHAR(20)     NOT NULL,
    units             NUMERIC(15,4),
    nav_price         NUMERIC(15,4),
    source_of_funds   VARCHAR(255),
    notes             TEXT,
    goal_id           UUID,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_txn_holding ON transaction(holding_id);
CREATE INDEX idx_txn_owner ON transaction(owner_id);
CREATE INDEX idx_txn_date ON transaction(transaction_date);

-- ─── Transaction Correction ─────────────────────────────────
CREATE TABLE transaction_correction (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    original_transaction_id  UUID            NOT NULL REFERENCES transaction(id),
    correction_date          DATE            NOT NULL,
    corrected_amount         NUMERIC(15,2),
    reason                   TEXT            NOT NULL,
    correction_type          VARCHAR(20)     NOT NULL,
    created_at               TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_txn_correction_original ON transaction_correction(original_transaction_id);

-- ═══════════════════════════════════════════════════════════════
-- SEED: Instrument Type Catalog (23 types from SPEC Section 14.1)
--
-- This is the pluggable catalog. To add a new instrument type:
--   INSERT INTO instrument_type (code, display_name, asset_class, ...)
--   VALUES ('NEW_TYPE', 'New Type', 'EQUITY', ...);
-- No code changes needed — the UI reads types from this table.
-- ═══════════════════════════════════════════════════════════════

INSERT INTO instrument_type (code, display_name, asset_class, tax_treatment_json, valuation_method, liquidity_profile, required_fields_json, optional_fields_json) VALUES
('MUTUAL_FUND', 'Mutual Fund', 'EQUITY', '{"stcg":"15%","ltcg":"10% above 1L","holding_period":"1 year for equity"}', 'NAV_TIMES_UNITS', 'HIGH', '["units","nav_price"]', '["ter","aum","fund_manager","tracking_error","exit_load"]'),
('DIRECT_EQUITY', 'Stocks', 'EQUITY', '{"stcg":"20% (<1yr)","ltcg":"12.5% (>1yr, >1.25L)"}', 'MARKET_PRICE_TIMES_QTY', 'HIGH', '["quantity","purchase_price"]', '["current_market_price","dividends_received"]'),
('EPF', 'Employee Provident Fund', 'DEBT', '{"treatment":"EEE up to 2.5L/yr contribution"}', 'BALANCE_STATEMENT', 'LOW', '["balance"]', '["employer_contribution","interest_rate"]'),
('VPF', 'Voluntary Provident Fund', 'DEBT', '{"treatment":"Same as EPF"}', 'BALANCE_STATEMENT', 'LOW', '["balance"]', '["interest_rate"]'),
('PPF', 'Public Provident Fund', 'DEBT', '{"treatment":"EEE fully exempt"}', 'BALANCE_STATEMENT', 'LOW', '["balance"]', '["maturity_date","interest_rate"]'),
('NPS_TIER1', 'NPS Tier I', 'MIXED', '{"deduction":"80CCD(1B) 50k extra","withdrawal":"partial tax"}', 'NAV_TIMES_UNITS', 'VERY_LOW', '["units","nav_price"]', '["asset_allocation"]'),
('NPS_TIER2', 'NPS Tier II', 'MIXED', '{"treatment":"No special tax benefit"}', 'NAV_TIMES_UNITS', 'MEDIUM', '["units","nav_price"]', '["asset_allocation"]'),
('FD', 'Fixed Deposit', 'DEBT', '{"interest":"Taxed as income","tds":"If >40k/yr"}', 'PRINCIPAL_PLUS_INTEREST', 'MEDIUM', '["principal","interest_rate","maturity_date"]', '["bank","auto_renew"]'),
('RD', 'Recurring Deposit', 'DEBT', '{"interest":"Taxed as income"}', 'MATURITY_PROJECTION', 'MEDIUM', '["monthly_amount","interest_rate","maturity_date"]', '["bank"]'),
('SGB', 'Sovereign Gold Bond', 'GOLD', '{"interest":"2.5% taxed","ltcg":"Exempt on maturity (8yr)"}', 'GOLD_PRICE_TIMES_GRAMS', 'LOW', '["grams","purchase_price"]', '["issue_date","maturity_date"]'),
('PHYSICAL_GOLD', 'Physical Gold', 'GOLD', '{"ltcg":"12.5% (>2yr)"}', 'WEIGHT_TIMES_MARKET_PRICE', 'MEDIUM', '["weight_grams","purchase_price"]', '["purity","storage"]'),
('GOLD_ETF', 'Gold ETF', 'GOLD', '{"ltcg":"12.5% (>1yr)"}', 'NAV_TIMES_UNITS', 'HIGH', '["units","nav_price"]', '["ter"]'),
('GOVT_BOND', 'Government Bond / G-Sec', 'DEBT', '{"interest":"Taxed","ltcg":"On sale"}', 'FACE_VALUE_OR_MARKET', 'MEDIUM', '["face_value","coupon_rate"]', '["maturity_date","yield_to_maturity"]'),
('CORP_BOND', 'Corporate Bond', 'DEBT', '{"interest":"Taxed","ltcg":"On sale"}', 'FACE_VALUE_OR_MARKET', 'MEDIUM', '["face_value","coupon_rate"]', '["credit_rating","maturity_date"]'),
('SCSS', 'Senior Citizens Savings Scheme', 'DEBT', '{"interest":"Taxed","deduction":"80C on investment"}', 'PRINCIPAL_PLUS_INTEREST', 'LOW', '["principal","maturity_date"]', '["interest_payout_frequency"]'),
('POMIS', 'Post Office MIS', 'DEBT', '{"interest":"Taxed monthly"}', 'PRINCIPAL_FIXED', 'LOW', '["principal","maturity_date"]', '["monthly_interest"]'),
('KVP', 'Kisan Vikas Patra', 'DEBT', '{"maturity":"Taxed"}', 'MATURITY_PROJECTION', 'LOW', '["principal","maturity_date"]', '["doubling_period"]'),
('NSC', 'National Savings Certificate', 'DEBT', '{"interest":"Taxed but 80C","reinvested_interest":"Gets 80C"}', 'FACE_VALUE_PLUS_ACCRUED', 'LOW', '["face_value","maturity_date"]', '["interest_rate"]'),
('ULIP', 'ULIP', 'MIXED', '{"treatment":"Exempt if premium <2.5L/yr else taxable"}', 'FUND_VALUE_STATEMENT', 'LOW', '["fund_value","premium"]', '["policy_term","lock_in_years"]'),
('ESOP', 'ESOPs / RSUs', 'EQUITY', '{"perquisite":"Tax on exercise","gains":"STCG/LTCG on sale"}', 'MARKET_PRICE_TIMES_VESTED', 'MEDIUM', '["vested_units","exercise_price"]', '["grant_date","vesting_schedule","current_market_price"]'),
('REIT', 'REIT', 'REAL_ESTATE', '{"dividend":"Partially taxable","ltcg":"On units"}', 'NAV_TIMES_UNITS', 'HIGH', '["units","nav_price"]', '["distribution_yield"]'),
('INVIT', 'InvIT', 'INFRASTRUCTURE', '{"treatment":"Similar to REIT"}', 'NAV_TIMES_UNITS', 'HIGH', '["units","nav_price"]', '["distribution_yield"]'),
('CRYPTO', 'Cryptocurrency', 'DIGITAL_ASSET', '{"gains":"30% flat tax","tds":"1% on transfer"}', 'MARKET_PRICE_TIMES_QTY', 'HIGH', '["quantity","purchase_price"]', '["exchange","wallet_address"]'),
('OTHER', 'Other', 'OTHER', '{"treatment":"Manual tax treatment entry"}', 'USER_ENTERED', 'MEDIUM', '["value"]', '["description"]');
