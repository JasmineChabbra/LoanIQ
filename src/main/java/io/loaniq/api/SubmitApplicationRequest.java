package io.loaniq.api;

import io.loaniq.ai.SupportingDocument;
import io.loaniq.domain.EmploymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Inbound application.
 *
 * <p>Declared figures are optional because they may instead arrive inside the
 * documents; anything the applicant does state is treated as a claim and is
 * overridden by extracted evidence where the two disagree.
 */
public record SubmitApplicationRequest(
        @NotBlank @Size(max = 64) String applicantRef,
        @NotBlank @Size(max = 160) String applicantName,
        @NotNull @DecimalMin("1000.00") BigDecimal amountRequested,
        @Min(1) @Max(480) int tenureMonths,
        BigDecimal declaredMonthlyIncome,
        BigDecimal monthlyObligations,
        EmploymentType employmentType,
        @Min(300) @Max(900) Integer bureauScore,
        List<SupportingDocument> documents) {}
