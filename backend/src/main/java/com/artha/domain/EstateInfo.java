package com.artha.domain;

import jakarta.persistence.*;

/** Estate/succession metadata per holding or account. SPEC REF: FR-7.5 */
@Entity
@Table(name = "estate_info")
public class EstateInfo extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "holding_id")
    private Holding holding;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "nominee_name")
    private String nomineeName;

    @Column(name = "joint_holders_json", columnDefinition = "jsonb")
    private String jointHoldersJson;

    @Column(name = "beneficiary_designation")
    private String beneficiaryDesignation;

    @Enumerated(EnumType.STRING)
    @Column(name = "will_status", nullable = false)
    private WillStatus willStatus = WillStatus.NO_WILL;

    // ─── Getters & Setters ───────────────────────────────────
    public Holding getHolding() { return holding; }
    public void setHolding(Holding holding) { this.holding = holding; }
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    public String getNomineeName() { return nomineeName; }
    public void setNomineeName(String nomineeName) { this.nomineeName = nomineeName; }
    public String getJointHoldersJson() { return jointHoldersJson; }
    public void setJointHoldersJson(String jointHoldersJson) { this.jointHoldersJson = jointHoldersJson; }
    public String getBeneficiaryDesignation() { return beneficiaryDesignation; }
    public void setBeneficiaryDesignation(String beneficiaryDesignation) { this.beneficiaryDesignation = beneficiaryDesignation; }
    public WillStatus getWillStatus() { return willStatus; }
    public void setWillStatus(WillStatus willStatus) { this.willStatus = willStatus; }
}
