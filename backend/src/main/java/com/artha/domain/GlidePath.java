package com.artha.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * GlidePath — Asset allocation schedule for a goal over time.
 *
 * SPEC REF: FR-4.3 — Hybrid glide-path engine.
 *
 * The schedule is stored as a JSON array of milestones:
 *   [{"yearsToGoal":8,"equityPct":70,"safePct":30},
 *    {"yearsToGoal":4,"equityPct":50,"safePct":50},
 *    {"yearsToGoal":1,"equityPct":20,"safePct":80}]
 *
 * For age-based paths (retirement), the milestones use "age" instead of "yearsToGoal".
 */
@Entity
@Table(name = "glide_path")
public class GlidePath extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", nullable = false, unique = true)
    private Goal goal;

    @Enumerated(EnumType.STRING)
    @Column(name = "glide_path_type", nullable = false)
    private GlidePathType glidePathType;

    /**
     * The schedule as a JSON array of allocation milestones.
     * Stored as JSONB in PostgreSQL for queryability.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schedule_json", columnDefinition = "jsonb", nullable = false)
    private String scheduleJson;

    // ─── Getters & Setters ───────────────────────────────────

    public Goal getGoal() { return goal; }
    public void setGoal(Goal goal) { this.goal = goal; }
    public GlidePathType getGlidePathType() { return glidePathType; }
    public void setGlidePathType(GlidePathType glidePathType) { this.glidePathType = glidePathType; }
    public String getScheduleJson() { return scheduleJson; }
    public void setScheduleJson(String scheduleJson) { this.scheduleJson = scheduleJson; }
}
