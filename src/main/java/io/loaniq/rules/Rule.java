package io.loaniq.rules;

import io.loaniq.domain.LoanApplication;
import io.loaniq.domain.RuleOutcome;

/**
 * One underwriting rule.
 *
 * <p>Rules are pure: they read an application and return an outcome without
 * mutating anything. That keeps them trivially unit-testable and lets the
 * engine run them in any order.
 */
public interface Rule {

    /** Stable code used in audit records and policy documents. */
    String code();

    /** Lower runs first. Hard stops are conventionally in the 0-99 band. */
    int order();

    RuleOutcome evaluate(LoanApplication application);
}
