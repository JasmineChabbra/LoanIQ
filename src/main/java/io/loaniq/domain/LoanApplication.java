package io.loaniq.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A credit application as it moves through underwriting.
 *
 * <p>The entity deliberately carries both the applicant-supplied facts and the
 * values the engine derived from them, so a decision can be reproduced later
 * from the row alone.
 */
@Entity
@Table(
        name = "loan_application",
        indexes = {
            @Index(name = "idx_loan_application_status", columnList = "status"),
            @Index(name = "idx_loan_application_applicant", columnList = "applicant_ref")
        })
public class LoanApplication {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Caller-supplied key that makes submission idempotent. */
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "applicant_ref", nullable = false, length = 64)
    private String applicantRef;

    @Column(name = "applicant_name", nullable = false, length = 160)
    private String applicantName;

    @Column(name = "amount_requested", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountRequested;

    @Column(name = "tenure_months", nullable = false)
    private int tenureMonths;

    @Column(name = "declared_monthly_income", precision = 15, scale = 2)
    private BigDecimal declaredMonthlyIncome;

    @Column(name = "monthly_obligations", precision = 15, scale = 2)
    private BigDecimal monthlyObligations;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", length = 32)
    private EmploymentType employmentType;

    @Column(name = "bureau_score")
    private Integer bureauScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ApplicationStatus status = ApplicationStatus.RECEIVED;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", length = 16)
    private Decision decision;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "approved_amount", precision = 15, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "decision_rationale", length = 4000)
    private String decisionRationale;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RuleOutcome> ruleOutcomes = new ArrayList<>();

    protected LoanApplication() {
        // for JPA
    }

    public LoanApplication(UUID id, String idempotencyKey, String applicantRef, String applicantName) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.applicantRef = applicantRef;
        this.applicantName = applicantName;
    }

    public void addRuleOutcome(RuleOutcome outcome) {
        outcome.setApplication(this);
        this.ruleOutcomes.add(outcome);
    }

    /**
     * Debt-to-income ratio as a fraction of declared income, or {@code null}
     * when income is unknown or zero. Callers must treat null as "cannot
     * assess" rather than "healthy".
     */
    public BigDecimal debtToIncomeRatio() {
        if (declaredMonthlyIncome == null || declaredMonthlyIncome.signum() <= 0) {
            return null;
        }
        BigDecimal obligations = monthlyObligations == null ? BigDecimal.ZERO : monthlyObligations;
        return obligations.divide(declaredMonthlyIncome, 4, java.math.RoundingMode.HALF_UP);
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getApplicantRef() {
        return applicantRef;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public BigDecimal getAmountRequested() {
        return amountRequested;
    }

    public void setAmountRequested(BigDecimal amountRequested) {
        this.amountRequested = amountRequested;
    }

    public int getTenureMonths() {
        return tenureMonths;
    }

    public void setTenureMonths(int tenureMonths) {
        this.tenureMonths = tenureMonths;
    }

    public BigDecimal getDeclaredMonthlyIncome() {
        return declaredMonthlyIncome;
    }

    public void setDeclaredMonthlyIncome(BigDecimal declaredMonthlyIncome) {
        this.declaredMonthlyIncome = declaredMonthlyIncome;
    }

    public BigDecimal getMonthlyObligations() {
        return monthlyObligations;
    }

    public void setMonthlyObligations(BigDecimal monthlyObligations) {
        this.monthlyObligations = monthlyObligations;
    }

    public EmploymentType getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(EmploymentType employmentType) {
        this.employmentType = employmentType;
    }

    public Integer getBureauScore() {
        return bureauScore;
    }

    public void setBureauScore(Integer bureauScore) {
        this.bureauScore = bureauScore;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(BigDecimal approvedAmount) {
        this.approvedAmount = approvedAmount;
    }

    public String getDecisionRationale() {
        return decisionRationale;
    }

    public void setDecisionRationale(String decisionRationale) {
        this.decisionRationale = decisionRationale;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    public List<RuleOutcome> getRuleOutcomes() {
        return ruleOutcomes;
    }
}
