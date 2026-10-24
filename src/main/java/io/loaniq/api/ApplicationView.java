package io.loaniq.api;

import io.loaniq.domain.ApplicationStatus;
import io.loaniq.domain.Decision;
import io.loaniq.domain.LoanApplication;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** What the caller sees back: the decision plus the reasoning behind it. */
public record ApplicationView(
        UUID id,
        String applicantRef,
        String applicantName,
        BigDecimal amountRequested,
        int tenureMonths,
        ApplicationStatus status,
        Decision decision,
        Integer riskScore,
        BigDecimal approvedAmount,
        String rationale,
        Instant createdAt,
        Instant decidedAt,
        List<RuleOutcomeView> ruleOutcomes) {

    public static ApplicationView from(LoanApplication a) {
        return new ApplicationView(
                a.getId(),
                a.getApplicantRef(),
                a.getApplicantName(),
                a.getAmountRequested(),
                a.getTenureMonths(),
                a.getStatus(),
                a.getDecision(),
                a.getRiskScore(),
                a.getApprovedAmount(),
                a.getDecisionRationale(),
                a.getCreatedAt(),
                a.getDecidedAt(),
                a.getRuleOutcomes().stream().map(RuleOutcomeView::from).toList());
    }
}
