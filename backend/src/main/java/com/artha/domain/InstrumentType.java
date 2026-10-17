package com.artha.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * InstrumentType — Pluggable catalog of financial instrument types.
 *
 * This is a REFERENCE TABLE — it defines what types of instruments exist,
 * not actual holdings. Holdings reference an InstrumentType via FK.
 *
 * SPEC REF: FR-3.1 — "Pluggable instrument type catalog stored in the database.
 * New types can be added via the database without code changes."
 *
 * WHY JSONB COLUMNS?
 *   Each instrument type has different metadata shapes:
 *   - Mutual Fund needs: TER, AUM, exit load schedule
 *   - FD needs: maturity date, interest payout frequency
 *   - Gold needs: weight, purity
 *   Using JSONB avoids 23 separate tables while keeping data queryable.
 *   The required_fields_json tells the UI which form fields to show.
 *
 * SEEDED IN: V2__investments.sql (23 types from spec Section 14.1)
 */
@Entity
@Table(name = "instrument_type")
public class InstrumentType extends BaseEntity {

    /**
     * Unique code (e.g., "MUTUAL_FUND", "DIRECT_EQUITY", "EPF").
     * Used as a stable identifier in code and API — display_name can change.
     */
    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    /**
     * Asset class: EQUITY, DEBT, GOLD, REAL_ESTATE, DIGITAL_ASSET, MIXED, OTHER.
     * Used for portfolio allocation and glide-path calculations.
     */
    @Column(name = "asset_class", nullable = false)
    private String assetClass;

    /**
     * Tax treatment rules as JSON. Varies per instrument type.
     * Example for MUTUAL_FUND: {"stcg":"15%","ltcg":"10% above 1L","holding_period":"1 year"}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tax_treatment_json", columnDefinition = "jsonb")
    private String taxTreatmentJson;

    /**
     * How to value this instrument: "NAV_TIMES_UNITS", "MARKET_PRICE_TIMES_QTY",
     * "BALANCE_STATEMENT", "PRINCIPAL_PLUS_INTEREST", "USER_ENTERED", etc.
     */
    @Column(name = "valuation_method")
    private String valuationMethod;

    /**
     * Maturity rules as JSON (for fixed-term instruments).
     * Example for PPF: {"tenure_years":15,"extendable":true,"extension_block_years":5}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "maturity_rules_json", columnDefinition = "jsonb")
    private String maturityRulesJson;

    /**
     * Liquidity profile: "HIGH" (stocks, liquid MF), "MEDIUM" (FD with penalty),
     * "LOW" (PPF lock-in), "VERY_LOW" (NPS Tier I).
     */
    @Column(name = "liquidity_profile")
    private String liquidityProfile;

    /**
     * Required fields as JSON array — tells the UI which fields are mandatory
     * when creating a holding of this type.
     * Example: ["units","nav_price","folio_number"]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "required_fields_json", columnDefinition = "jsonb")
    private String requiredFieldsJson;

    /**
     * Optional fields as JSON array — shown in UI but not mandatory.
     * Example: ["ter","aum","fund_manager","tracking_error"]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "optional_fields_json", columnDefinition = "jsonb")
    private String optionalFieldsJson;

    // ─── Getters & Setters ───────────────────────────────────

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAssetClass() { return assetClass; }
    public void setAssetClass(String assetClass) { this.assetClass = assetClass; }

    public String getTaxTreatmentJson() { return taxTreatmentJson; }
    public void setTaxTreatmentJson(String taxTreatmentJson) { this.taxTreatmentJson = taxTreatmentJson; }

    public String getValuationMethod() { return valuationMethod; }
    public void setValuationMethod(String valuationMethod) { this.valuationMethod = valuationMethod; }

    public String getMaturityRulesJson() { return maturityRulesJson; }
    public void setMaturityRulesJson(String maturityRulesJson) { this.maturityRulesJson = maturityRulesJson; }

    public String getLiquidityProfile() { return liquidityProfile; }
    public void setLiquidityProfile(String liquidityProfile) { this.liquidityProfile = liquidityProfile; }

    public String getRequiredFieldsJson() { return requiredFieldsJson; }
    public void setRequiredFieldsJson(String requiredFieldsJson) { this.requiredFieldsJson = requiredFieldsJson; }

    public String getOptionalFieldsJson() { return optionalFieldsJson; }
    public void setOptionalFieldsJson(String optionalFieldsJson) { this.optionalFieldsJson = optionalFieldsJson; }
}
