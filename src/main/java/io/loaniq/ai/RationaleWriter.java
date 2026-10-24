package io.loaniq.ai;

import io.loaniq.domain.LoanApplication;
import io.loaniq.rules.RuleEngine;

/**
 * Turns a completed evaluation into prose an applicant can read.
 *
 * <p>The writer is given the decision; it never chooses one. That separation
 * is what keeps the system explainable: the sentence always describes the
 * rules that actually fired.
 */
public interface RationaleWriter {

    String write(LoanApplication application, RuleEngine.Evaluation evaluation);
}
