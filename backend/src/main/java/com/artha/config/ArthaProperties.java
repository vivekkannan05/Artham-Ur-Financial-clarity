package com.artha.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Artha-specific configuration properties.
 *
 * These bind to the 'artha:' section in application.yml.
 * Example: artha.currency-scale → getCurrencyScale()
 *
 * HOW @ConfigurationProperties WORKS:
 *   Spring Boot reads application.yml, finds the 'artha' prefix,
 *   and maps each property to a setter method on this class.
 *   The naming follows "relaxed binding" — kebab-case in YAML maps
 *   to camelCase in Java:
 *     artha.currency-scale  →  setCurrencyScale(...)
 *     artha.pan.encryption-key  →  getPan().setEncryptionKey(...)
 *
 * SPEC REFS: NFR-1.2 (precision), NFR-2.2 (stale data), NFR-8.2 (CORS)
 */
@Configuration
@ConfigurationProperties(prefix = "artha")
public class ArthaProperties {

    /** BigDecimal scale for currency amounts (per NFR-1.2: CURRENCY_SCALE=2) */
    private int currencyScale = 2;

    /** BigDecimal scale for rates/percentages (per NFR-1.2: RATE_SCALE=6) */
    private int rateScale = 6;

    /** Stale data threshold in days (per NFR-2.2: 90 days) */
    private int staleDataThresholdDays = 90;

    private Cors cors = new Cors();
    private Pan pan = new Pan();

    // ─── Nested config classes ───────────────────────────────

    public static class Cors {
        private String allowedOrigins = "http://localhost:3000";

        public String getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(String allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }

    public static class Pan {
        private String encryptionKey = "";

        public String getEncryptionKey() { return encryptionKey; }
        public void setEncryptionKey(String encryptionKey) { this.encryptionKey = encryptionKey; }
    }

    // ─── Getters & Setters ───────────────────────────────────

    public int getCurrencyScale() { return currencyScale; }
    public void setCurrencyScale(int currencyScale) { this.currencyScale = currencyScale; }

    public int getRateScale() { return rateScale; }
    public void setRateScale(int rateScale) { this.rateScale = rateScale; }

    public int getStaleDataThresholdDays() { return staleDataThresholdDays; }
    public void setStaleDataThresholdDays(int staleDataThresholdDays) { this.staleDataThresholdDays = staleDataThresholdDays; }

    public Cors getCors() { return cors; }
    public void setCors(Cors cors) { this.cors = cors; }

    public Pan getPan() { return pan; }
    public void setPan(Pan pan) { this.pan = pan; }
}
