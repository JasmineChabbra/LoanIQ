package io.loaniq.ai;

import io.loaniq.domain.Decision;
import io.loaniq.domain.LoanApplication;
import io.loaniq.domain.RuleOutcome;
import io.loaniq.rules.RuleEngine;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Builds the rationale from the rule outcomes alone.
 *
 * <p>Used when no model provider is configured. Because it only ever restates
 * rules that actually fired, its output is the baseline every model-backed
 * writer is checked against.
 */
@Component
@ConditionalOnMissingBean(name = "chatRationaleWriter")
public class TemplateRationaleWriter implements RationaleWriter {

    @Override
    public String write(LoanApplication application, RuleEngine.Evaluation evaluation) {
        if (evaluation.hardStopReason() != null) {
            return "Declined. " + evaluation.hardStopReason();
        }

        List<RuleOutcome> failed = evaluation.failedOutcomes();
        String headline =
                switch (evaluation.decision()) {
                    case APPROVE -> "Approved with a risk score of %d.".formatted(evaluation.score());
                    case REFER -> "Referred for manual review with a risk score of %d."
                            .formatted(evaluation.score());
                    case DECLINE -> "Declined with a risk score of %d.".formatted(evaluation.score());
                };

        if (failed.isEmpty()) {
            return headline + " Every policy check passed.";
        }

        String concerns = failed.stream().map(RuleOutcome::getExplanation).collect(Collectors.joining(" "));
        String verb = evaluation.decision() == Decision.APPROVE ? "Noted, but not disqualifying: " : "Driven by: ";
        return headline + " " + verb + concerns;
    }
}
