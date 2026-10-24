package io.loaniq.api;

import io.loaniq.domain.RuleOutcome;

public record RuleOutcomeView(String ruleCode, boolean passed, int scoreDelta, boolean hardStop, String explanation) {

    public static RuleOutcomeView from(RuleOutcome outcome) {
        return new RuleOutcomeView(
                outcome.getRuleCode(),
                outcome.isPassed(),
                outcome.getScoreDelta(),
                outcome.isHardStop(),
                outcome.getExplanation());
    }
}
