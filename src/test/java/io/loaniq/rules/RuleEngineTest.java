package io.loaniq.rules;

import static org.assertj.core.api.Assertions.assertThat;

import io.loaniq.config.UnderwritingProperties;
import io.loaniq.domain.Decision;
import io.loaniq.domain.EmploymentType;
import io.loaniq.domain.LoanApplication;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RuleEngineTest {

    private static final UnderwritingProperties PROPS = new UnderwritingProperties(
            620, new BigDecimal("0.45"), new BigDecimal("4.0"), 6, 84, 580, 520, 500);

    private final RuleEngine engine = new RuleEngine(
            List.of(
                    new MinimumBureauScoreRule(PROPS),
                    new DebtToIncomeRule(PROPS),
                    new LoanToIncomeRule(PROPS),
                    new EmploymentStabilityRule(),
                    new TenureRule(PROPS)),
            PROPS);

    private static LoanApplication application() {
        LoanApplication a = new LoanApplication(UUID.randomUUID(), "key-" + UUID.randomUUID(), "APP-1", "Test Person");
        a.setAmountRequested(new BigDecimal("300000"));
        a.setTenureMonths(36);
        a.setDeclaredMonthlyIncome(new BigDecimal("90000"));
        a.setMonthlyObligations(new BigDecimal("15000"));
        a.setEmploymentType(EmploymentType.SALARIED);
        a.setBureauScore(760);
        return a;
    }

    @Test
    @DisplayName("a clean salaried applicant is approved")
    void approvesStrongApplicant() {
        var result = engine.evaluate(application());

        assertThat(result.decision()).isEqualTo(Decision.APPROVE);
        assertThat(result.score()).isGreaterThanOrEqualTo(580);
        assertThat(result.failedOutcomes()).isEmpty();
    }

    @Test
    @DisplayName("a bureau score under the floor is a hard decline regardless of everything else")
    void hardDeclinesBelowBureauFloor() {
        LoanApplication a = application();
        a.setBureauScore(540);

        var result = engine.evaluate(a);

        assertThat(result.decision()).isEqualTo(Decision.DECLINE);
        assertThat(result.hardStopReason()).contains("below the hard floor");
        // The run stops, but the rules that already executed are still reported.
        assertThat(result.outcomes()).isNotEmpty();
    }

    @Test
    @DisplayName("unemployment is a hard stop even with a strong bureau score")
    void hardDeclinesUnemployed() {
        LoanApplication a = application();
        a.setEmploymentType(EmploymentType.UNEMPLOYED);

        var result = engine.evaluate(a);

        assertThat(result.decision()).isEqualTo(Decision.DECLINE);
        assertThat(result.hardStopReason()).contains("no serviceable income");
    }

    @Test
    @DisplayName("a missing bureau score refers rather than silently approving")
    void refersThinFile() {
        LoanApplication a = application();
        a.setBureauScore(null);

        var result = engine.evaluate(a);

        assertThat(result.decision()).isNotEqualTo(Decision.APPROVE);
        assertThat(result.failedOutcomes())
                .anyMatch(o -> o.getRuleCode().equals("BUREAU_MIN"));
    }

    @Test
    @DisplayName("obligations above the DTI ceiling pull the score down")
    void penalisesHighDebtToIncome() {
        LoanApplication a = application();
        a.setMonthlyObligations(new BigDecimal("60000")); // 0.67 of income

        var result = engine.evaluate(a);

        assertThat(result.failedOutcomes()).anyMatch(o -> o.getRuleCode().equals("DTI"));
        assertThat(result.score()).isLessThan(engine.evaluate(application()).score());
    }

    @Test
    @DisplayName("borrowing far beyond annual income is penalised even with no existing debt")
    void penalisesHighLoanToIncome() {
        LoanApplication a = application();
        a.setMonthlyObligations(BigDecimal.ZERO);
        a.setAmountRequested(new BigDecimal("9000000")); // ~8.3x annual

        var result = engine.evaluate(a);

        assertThat(result.failedOutcomes()).anyMatch(o -> o.getRuleCode().equals("LTI"));
    }

    @Test
    @DisplayName("tenure outside the product range is a hard stop")
    void hardStopsOnUnsupportedTenure() {
        LoanApplication a = application();
        a.setTenureMonths(120);

        var result = engine.evaluate(a);

        assertThat(result.decision()).isEqualTo(Decision.DECLINE);
        assertThat(result.hardStopReason()).contains("outside the supported");
    }

    @Test
    @DisplayName("the same application always produces the same decision")
    void isDeterministic() {
        LoanApplication a = application();

        var first = engine.evaluate(a);
        var second = engine.evaluate(a);

        assertThat(second.decision()).isEqualTo(first.decision());
        assertThat(second.score()).isEqualTo(first.score());
    }
}
