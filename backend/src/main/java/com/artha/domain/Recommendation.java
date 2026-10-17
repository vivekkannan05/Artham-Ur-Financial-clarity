package com.artha.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Recommendation — An advisor engine output stored for user decision.
 * SPEC REF: FR-7.2 — "recommendation history: trigger, evidence, action, status."
 */
@Entity
@Table(name = "recommendation")
public class Recommendation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Column(name = "trigger_description", nullable = false, columnDefinition = "TEXT")
    private String triggerDescription;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private Severity severity;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "evidence_json", columnDefinition = "jsonb")
    private String evidenceJson;

    @Column(name = "action", nullable = false, columnDefinition = "TEXT")
    private String action;

    @Column(name = "expected_impact", columnDefinition = "TEXT")
    private String expectedImpact;

    @Column(name = "downside_risk", columnDefinition = "TEXT")
    private String downsideRisk;

    @Column(name = "ai_explanation", columnDefinition = "TEXT")
    private String aiExplanation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RecommendationStatus status = RecommendationStatus.PENDING;

    @Column(name = "user_decision", columnDefinition = "TEXT")
    private String userDecision;

    @Column(name = "decision_timestamp")
    private Instant decisionTimestamp;

    @Column(name = "spec_version", length = 20)
    private String specVersion;

    // ─── Getters & Setters ───────────────────────────────────

    public Household getHousehold() { return household; }
    public void setHousehold(Household household) { this.household = household; }
    public String getTriggerDescription() { return triggerDescription; }
    public void setTriggerDescription(String triggerDescription) { this.triggerDescription = triggerDescription; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getEvidenceJson() { return evidenceJson; }
    public void setEvidenceJson(String evidenceJson) { this.evidenceJson = evidenceJson; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getExpectedImpact() { return expectedImpact; }
    public void setExpectedImpact(String expectedImpact) { this.expectedImpact = expectedImpact; }
    public String getDownsideRisk() { return downsideRisk; }
    public void setDownsideRisk(String downsideRisk) { this.downsideRisk = downsideRisk; }
    public String getAiExplanation() { return aiExplanation; }
    public void setAiExplanation(String aiExplanation) { this.aiExplanation = aiExplanation; }
    public RecommendationStatus getStatus() { return status; }
    public void setStatus(RecommendationStatus status) { this.status = status; }
    public String getUserDecision() { return userDecision; }
    public void setUserDecision(String userDecision) { this.userDecision = userDecision; }
    public Instant getDecisionTimestamp() { return decisionTimestamp; }
    public void setDecisionTimestamp(Instant decisionTimestamp) { this.decisionTimestamp = decisionTimestamp; }
    public String getSpecVersion() { return specVersion; }
    public void setSpecVersion(String specVersion) { this.specVersion = specVersion; }
}
