package io.loaniq.domain;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * The result of one rule evaluated against one application.
 *
 * <p>Every rule that runs records a row, including the ones that passed, so an
 * auditor can see what was considered and not only what failed.
 */
@Entity
@Table(name = "rule_outcome")
public class RuleOutcome {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private LoanApplication application;

    @Column(name = "rule_code", nullable = false, length = 64)
    private String ruleCode;

    @Column(name = "passed", nullable = false)
    private boolean passed;

    /** Negative for penalties, positive for credit. Applied to the base score. */
    @Column(name = "score_delta", nullable = false)
    private int scoreDelta;

    /** Hard declines stop the engine regardless of the running score. */
    @Column(name = "hard_stop", nullable = false)
    private boolean hardStop;

    @Column(name = "explanation", nullable = false, length = 500)
    private String explanation;

    protected RuleOutcome() {
        // for JPA
    }

    public RuleOutcome(String ruleCode, boolean passed, int scoreDelta, boolean hardStop, String explanation) {
        this.ruleCode = ruleCode;
        this.passed = passed;
        this.scoreDelta = scoreDelta;
        this.hardStop = hardStop;
        this.explanation = explanation;
    }

    public UUID getId() {
        return id;
    }

    public LoanApplication getApplication() {
        return application;
    }

    void setApplication(LoanApplication application) {
        this.application = application;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public boolean isPassed() {
        return passed;
    }

    public int getScoreDelta() {
        return scoreDelta;
    }

    public boolean isHardStop() {
        return hardStop;
    }

    public String getExplanation() {
        return explanation;
    }
}
