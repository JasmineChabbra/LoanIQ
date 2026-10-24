package io.loaniq.rules;

import io.loaniq.config.UnderwritingProperties;
import io.loaniq.domain.LoanApplication;
import io.loaniq.domain.RuleOutcome;
import org.springframework.stereotype.Component;

/** Keeps tenure inside the product's supported range. */
@Component
public class TenureRule implements Rule {

    private final UnderwritingProperties props;

    public TenureRule(UnderwritingProperties props) {
        this.props = props;
    }

    @Override
    public String code() {
        return "TENURE";
    }

    @Override
    public int order() {
        return 50;
    }

    @Override
    public RuleOutcome evaluate(LoanApplication application) {
        int tenure = application.getTenureMonths();
        int min = props.minTenureMonths();
        int max = props.maxTenureMonths();
        if (tenure < min || tenure > max) {
            return new RuleOutcome(
                    code(),
                    false,
                    0,
                    true,
                    "Tenure of %d months is outside the supported %d-%d range.".formatted(tenure, min, max));
        }
        return new RuleOutcome(
                code(), true, 5, false, "Tenure of %d months is within the supported range.".formatted(tenure));
    }
}
