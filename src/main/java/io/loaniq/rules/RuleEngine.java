package io.loaniq.rules;

import io.loaniq.config.UnderwritingProperties;
import io.loaniq.domain.Decision;
import io.loaniq.domain.LoanApplication;
import io.loaniq.domain.RuleOutcome;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Runs every rule and turns the outcomes into a score and a decision.
 *
 * <p>The engine is deliberately deterministic. The language model elsewhere in
 * this service reads documents and writes prose; it never participates here,
 * so the same application always produces the same decision and the result can
 * be replayed during an audit.
 */
@Component
public class RuleEngine {

    private final List<Rule> rules;
    private final UnderwritingProperties props;

    public RuleEngine(List<Rule> rules, UnderwritingProperties props) {
        this.rules = rules.stream().sorted(Comparator.comparingInt(Rule::order)).toList();
        this.props = props;
    }

    /**
     * Evaluates every rule, in order.
     *
     * <p>A hard stop short-circuits scoring but the rules that already ran are
     * still returned, so the audit trail shows the full picture rather than
     * only the rule that failed.
     */
    public Evaluation evaluate(LoanApplication application) {
        List<RuleOutcome> outcomes = new ArrayList<>();
        int score = props.baseScore();

        for (Rule rule : rules) {
            RuleOutcome outcome = rule.evaluate(application);
            outcomes.add(outcome);
            if (outcome.isHardStop()) {
                return new Evaluation(Decision.DECLINE, 0, outcomes, outcome.getExplanation());
            }
            score += outcome.getScoreDelta();
        }

        int bounded = Math.max(0, Math.min(1000, score));
        Decision decision;
        if (bounded >= props.approveThreshold()) {
            decision = Decision.APPROVE;
        } else if (bounded >= props.referThreshold()) {
            decision = Decision.REFER;
        } else {
            decision = Decision.DECLINE;
        }
        return new Evaluation(decision, bounded, outcomes, null);
    }

    /**
     * @param hardStopReason set only when a rule stopped the run outright
     */
    public record Evaluation(Decision decision, int score, List<RuleOutcome> outcomes, String hardStopReason) {

        public List<RuleOutcome> failedOutcomes() {
            return outcomes.stream().filter(o -> !o.isPassed()).toList();
        }
    }
}
