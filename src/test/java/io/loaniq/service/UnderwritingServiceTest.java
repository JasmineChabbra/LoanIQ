package io.loaniq.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.loaniq.ai.SupportingDocument;
import io.loaniq.api.SubmitApplicationRequest;
import io.loaniq.audit.AuditTrail;
import io.loaniq.domain.Decision;
import io.loaniq.domain.EmploymentType;
import io.loaniq.domain.LoanApplication;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class UnderwritingServiceTest {

    @Autowired
    private UnderwritingService service;

    @Autowired
    private AuditTrail auditTrail;

    private static SubmitApplicationRequest request(List<SupportingDocument> docs) {
        return new SubmitApplicationRequest(
                "APP-" + UUID.randomUUID(),
                "Test Person",
                new BigDecimal("300000"),
                36,
                new BigDecimal("90000"),
                new BigDecimal("15000"),
                EmploymentType.SALARIED,
                760,
                docs);
    }

    @Test
    @DisplayName("a submitted application comes back decided with its rule outcomes")
    void decidesOnSubmit() {
        LoanApplication result = service.submit(UUID.randomUUID().toString(), request(null));

        assertThat(result.getDecision()).isNotNull();
        assertThat(result.getRiskScore()).isNotNull();
        assertThat(result.getRuleOutcomes()).isNotEmpty();
        assertThat(result.getDecisionRationale()).isNotBlank();
        assertThat(result.getDecidedAt()).isNotNull();
    }

    @Test
    @DisplayName("resubmitting the same idempotency key replays the original decision")
    void isIdempotent() {
        String key = UUID.randomUUID().toString();
        var req = request(null);

        LoanApplication first = service.submit(key, req);
        LoanApplication second = service.submit(key, req);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getDecision()).isEqualTo(first.getDecision());
        assertThat(second.getRiskScore()).isEqualTo(first.getRiskScore());
    }

    @Test
    @DisplayName("facts read from documents override what the applicant declared")
    void extractedFactsOverrideDeclaredOnes() {
        var docs = List.of(new SupportingDocument(
                "payslip",
                """
                ACME Corp payslip - issued by employer
                Net monthly salary: 42,000.00
                Existing EMI: 31,000.00
                Bureau score: 705
                """));
        var req = new SubmitApplicationRequest(
                "APP-" + UUID.randomUUID(),
                "Test Person",
                new BigDecimal("300000"),
                36,
                new BigDecimal("90000"), // declared, but the payslip says otherwise
                BigDecimal.ZERO,
                EmploymentType.SALARIED,
                800,
                docs);

        LoanApplication result = service.submit(UUID.randomUUID().toString(), req);

        assertThat(result.getDeclaredMonthlyIncome()).isEqualByComparingTo("42000.00");
        assertThat(result.getMonthlyObligations()).isEqualByComparingTo("31000.00");
        assertThat(result.getBureauScore()).isEqualTo(705);
    }

    @Test
    @DisplayName("every decision leaves a replayable audit trail")
    void writesAuditTrail() {
        LoanApplication result = service.submit(UUID.randomUUID().toString(), request(null));

        var events = auditTrail.forApplication(result.getId());

        assertThat(events).extracting("eventType").contains("RECEIVED", "DECIDED");
    }

    @Test
    @DisplayName("an unemployed applicant is declined with a zero approved amount")
    void declinesUnemployed() {
        var req = new SubmitApplicationRequest(
                "APP-" + UUID.randomUUID(),
                "Test Person",
                new BigDecimal("300000"),
                36,
                new BigDecimal("90000"),
                BigDecimal.ZERO,
                EmploymentType.UNEMPLOYED,
                800,
                null);

        LoanApplication result = service.submit(UUID.randomUUID().toString(), req);

        assertThat(result.getDecision()).isEqualTo(Decision.DECLINE);
        assertThat(result.getApprovedAmount()).isEqualByComparingTo("0");
    }
}
