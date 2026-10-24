package io.loaniq.rules;

import io.loaniq.config.UnderwritingProperties;
import io.loaniq.domain.LoanApplication;
import io.loaniq.domain.RuleOutcome;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Penalises applicants whose existing obligations already eat their income. */
@Component
public class DebtToIncomeRule implements Rule {

    private final UnderwritingProperties props;

    public DebtToIncomeRule(UnderwritingProperties props) {
        this.props = props;
    }

    @Override
    public String code() {
        return "DTI";
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public RuleOutcome evaluate(LoanApplication application) {
        BigDecimal dti = application.debtToIncomeRatio();
        if (dti == null) {
            return new RuleOutcome(
                    code(), false, -25, false, "Income not stated, so debt-to-income could not be computed.");
        }
        BigDecimal ceiling = props.maxDebtToIncome();
        if (dti.compareTo(ceiling) > 0) {
            return new RuleOutcome(
                    code(),
                    false,
                    -45,
                    false,
                    "Debt-to-income %.2f exceeds the %.2f ceiling.".formatted(dti, ceiling));
        }
        BigDecimal headroom = ceiling.subtract(dti);
        int delta = headroom.multiply(BigDecimal.valueOf(60)).intValue();
        return new RuleOutcome(
                code(),
                true,
                Math.min(delta, 30),
                false,
                "Debt-to-income %.2f is within the %.2f ceiling.".formatted(dti, ceiling));
    }
}
