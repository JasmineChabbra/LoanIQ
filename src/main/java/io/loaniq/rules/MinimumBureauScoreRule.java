package io.loaniq.rules;

import io.loaniq.config.UnderwritingProperties;
import io.loaniq.domain.LoanApplication;
import io.loaniq.domain.RuleOutcome;
import org.springframework.stereotype.Component;

/**
 * Declines outright below the bureau floor.
 *
 * <p>A missing score is not treated as a pass: it refers instead, because an
 * applicant with no bureau history is a different risk from one with a good
 * score, and silently approving them is how thin-file fraud gets through.
 */
@Component
public class MinimumBureauScoreRule implements Rule {

    private final UnderwritingProperties props;

    public MinimumBureauScoreRule(UnderwritingProperties props) {
        this.props = props;
    }

    @Override
    public String code() {
        return "BUREAU_MIN";
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public RuleOutcome evaluate(LoanApplication application) {
        Integer score = application.getBureauScore();
        if (score == null) {
            return new RuleOutcome(
                    code(), false, -40, false, "No bureau score on file; cannot assess credit history.");
        }
        int floor = props.minimumBureauScore();
        if (score < floor) {
            return new RuleOutcome(
                    code(),
                    false,
                    0,
                    true,
                    "Bureau score %d is below the hard floor of %d.".formatted(score, floor));
        }
        int delta = Math.min(40, (score - floor) / 5);
        return new RuleOutcome(
                code(), true, delta, false, "Bureau score %d clears the floor of %d.".formatted(score, floor));
    }
}
