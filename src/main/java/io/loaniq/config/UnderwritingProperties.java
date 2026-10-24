package io.loaniq.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Credit policy thresholds.
 *
 * <p>These live in configuration rather than in code so a credit team can move
 * a threshold without a release, and so the active policy is visible in one
 * place when a regulator asks why a decision came out the way it did.
 */
@Validated
@ConfigurationProperties(prefix = "loaniq.underwriting")
public record UnderwritingProperties(
        @Min(300) int minimumBureauScore,
        @NotNull BigDecimal maxDebtToIncome,
        @NotNull BigDecimal maxLoanToIncomeMultiple,
        @Min(1) int minTenureMonths,
        @Min(1) int maxTenureMonths,
        @Min(0) int approveThreshold,
        @Min(0) int referThreshold,
        @Min(0) int baseScore) {

    public UnderwritingProperties {
        if (maxTenureMonths < minTenureMonths) {
            throw new IllegalArgumentException("maxTenureMonths must be >= minTenureMonths");
        }
        if (approveThreshold < referThreshold) {
            // Guard against a config edit that would make REFER unreachable.
            throw new IllegalArgumentException("approveThreshold must be >= referThreshold");
        }
    }
}
