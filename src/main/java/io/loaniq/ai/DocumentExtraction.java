package io.loaniq.ai;

import io.loaniq.domain.EmploymentType;
import java.math.BigDecimal;

/**
 * Facts a model pulled out of an applicant's documents.
 *
 * <p>Every field is nullable: the extractor is expected to leave a field unset
 * rather than guess, and the rule engine treats "unknown" as a risk in its own
 * right. {@code confidence} is the extractor's own estimate and is recorded in
 * the audit trail, not used to gate the decision.
 */
public record DocumentExtraction(
        BigDecimal monthlyIncome,
        BigDecimal monthlyObligations,
        EmploymentType employmentType,
        Integer bureauScore,
        double confidence,
        String notes) {

    public static DocumentExtraction empty(String reason) {
        return new DocumentExtraction(null, null, null, null, 0.0, reason);
    }

    public boolean hasAnyFact() {
        return monthlyIncome != null || monthlyObligations != null || employmentType != null || bureauScore != null;
    }
}
