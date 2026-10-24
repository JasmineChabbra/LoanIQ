package io.loaniq.rules;

import io.loaniq.config.UnderwritingProperties;
import io.loaniq.domain.LoanApplication;
import io.loaniq.domain.RuleOutcome;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * Caps the loan against annualised income.
 *
 * <p>Separate from debt-to-income on purpose: an applicant can carry no
 * existing debt and still be asking for more than they can ever repay.
 */
@Component
public class LoanToIncomeRule implements Rule {

    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);

    private final UnderwritingProperties props;

    public LoanToIncomeRule(UnderwritingProperties props) {
        this.props = props;
    }

    @Override
    public String code() {
        return "LTI";
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public RuleOutcome evaluate(LoanApplication application) {
        BigDecimal income = application.getDeclaredMonthlyIncome();
        if (income == null || income.signum() <= 0) {
            return new RuleOutcome(code(), false, -20, false, "Income not stated; loan-to-income not assessable.");
        }
        BigDecimal annual = income.multiply(MONTHS_PER_YEAR);
        BigDecimal multiple = application.getAmountRequested().divide(annual, 2, RoundingMode.HALF_UP);
        BigDecimal ceiling = props.maxLoanToIncomeMultiple();
        if (multiple.compareTo(ceiling) > 0) {
            return new RuleOutcome(
                    code(),
                    false,
                    -35,
                    false,
                    "Requested amount is %.2fx annual income, above the %.2fx ceiling.".formatted(multiple, ceiling));
        }
        return new RuleOutcome(
                code(),
                true,
                20,
                false,
                "Requested amount is %.2fx annual income, within the %.2fx ceiling.".formatted(multiple, ceiling));
    }
}
