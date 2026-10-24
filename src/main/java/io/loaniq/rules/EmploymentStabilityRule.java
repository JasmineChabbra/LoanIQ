package io.loaniq.rules;

import io.loaniq.domain.EmploymentType;
import io.loaniq.domain.LoanApplication;
import io.loaniq.domain.RuleOutcome;
import org.springframework.stereotype.Component;

/** Weighs income predictability by employment type. */
@Component
public class EmploymentStabilityRule implements Rule {

    @Override
    public String code() {
        return "EMPLOYMENT";
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public RuleOutcome evaluate(LoanApplication application) {
        EmploymentType type = application.getEmploymentType();
        if (type == null) {
            return new RuleOutcome(code(), false, -15, false, "Employment type not supplied.");
        }
        return switch (type) {
            case SALARIED -> new RuleOutcome(code(), true, 20, false, "Salaried income is predictable.");
            case CONTRACT -> new RuleOutcome(
                    code(), true, 5, false, "Contract income is serviceable but less predictable than salaried.");
            case SELF_EMPLOYED -> new RuleOutcome(
                    code(), true, 0, false, "Self-employed income varies; no uplift applied.");
            case UNEMPLOYED -> new RuleOutcome(
                    code(), false, 0, true, "Applicant is unemployed; no serviceable income source.");
        };
    }
}
