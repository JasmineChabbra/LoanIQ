package io.loaniq.service;

import io.loaniq.ai.DocumentExtraction;
import io.loaniq.ai.DocumentExtractor;
import io.loaniq.ai.RationaleWriter;
import io.loaniq.api.SubmitApplicationRequest;
import io.loaniq.audit.AuditTrail;
import io.loaniq.domain.*;
import io.loaniq.repo.LoanApplicationRepository;
import io.loaniq.rules.RuleEngine;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates one application from intake to decision.
 *
 * <p>The order matters: documents are read first so that extracted evidence
 * can override what the applicant declared, then the deterministic rule engine
 * decides, and only then is prose generated. A model never sees the decision
 * before it is made.
 */
@Service
public class UnderwritingService {

    private static final Logger log = LoggerFactory.getLogger(UnderwritingService.class);
    private static final String ACTOR = "underwriting-engine";

    private final LoanApplicationRepository repository;
    private final DocumentExtractor extractor;
    private final RuleEngine ruleEngine;
    private final RationaleWriter rationaleWriter;
    private final AuditTrail auditTrail;

    public UnderwritingService(
            LoanApplicationRepository repository,
            DocumentExtractor extractor,
            RuleEngine ruleEngine,
            RationaleWriter rationaleWriter,
            AuditTrail auditTrail) {
        this.repository = repository;
        this.extractor = extractor;
        this.ruleEngine = ruleEngine;
        this.rationaleWriter = rationaleWriter;
        this.auditTrail = auditTrail;
    }

    /**
     * Submits and underwrites an application.
     *
     * <p>Re-submitting the same idempotency key returns the original decision
     * rather than scoring again. Without that, a client retry after a timeout
     * would create a second application and could produce a different answer.
     */
    @Transactional
    public LoanApplication submit(String idempotencyKey, SubmitApplicationRequest request) {
        var existing = repository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Replaying stored decision for idempotency key {}", idempotencyKey);
            return existing.get();
        }

        LoanApplication application =
                new LoanApplication(UUID.randomUUID(), idempotencyKey, request.applicantRef(), request.applicantName());
        application.setAmountRequested(request.amountRequested());
        application.setTenureMonths(request.tenureMonths());
        application.setDeclaredMonthlyIncome(request.declaredMonthlyIncome());
        application.setMonthlyObligations(request.monthlyObligations());
        application.setEmploymentType(request.employmentType());
        application.setBureauScore(request.bureauScore());
        repository.save(application);
        auditTrail.record(application.getId(), "RECEIVED", ACTOR, "Application accepted for underwriting.");

        applyExtraction(application, request);
        decide(application);
        return repository.save(application);
    }

    private void applyExtraction(LoanApplication application, SubmitApplicationRequest request) {
        List<io.loaniq.ai.SupportingDocument> documents = request.documents();
        if (documents == null || documents.isEmpty()) {
            auditTrail.record(
                    application.getId(), "EXTRACTION_SKIPPED", ACTOR, "No supporting documents were supplied.");
            return;
        }

        application.setStatus(ApplicationStatus.EXTRACTING);
        DocumentExtraction extraction;
        try {
            extraction = extractor.extract(documents);
        } catch (RuntimeException e) {
            // Extraction is an enrichment step. If it fails we still underwrite
            // on declared figures rather than failing the whole application.
            log.warn("Document extraction failed for application {}", application.getId(), e);
            auditTrail.record(
                    application.getId(),
                    "EXTRACTION_FAILED",
                    ACTOR,
                    "Extraction threw %s; continuing on declared figures.".formatted(e.getClass().getSimpleName()));
            return;
        }

        if (!extraction.hasAnyFact()) {
            auditTrail.record(
                    application.getId(), "EXTRACTION_EMPTY", ACTOR, "No usable facts found. " + extraction.notes());
            return;
        }

        StringBuilder applied = new StringBuilder("Extracted facts applied:");
        if (extraction.monthlyIncome() != null) {
            applied.append(" income=").append(extraction.monthlyIncome());
            application.setDeclaredMonthlyIncome(extraction.monthlyIncome());
        }
        if (extraction.monthlyObligations() != null) {
            applied.append(" obligations=").append(extraction.monthlyObligations());
            application.setMonthlyObligations(extraction.monthlyObligations());
        }
        if (extraction.employmentType() != null) {
            applied.append(" employment=").append(extraction.employmentType());
            application.setEmploymentType(extraction.employmentType());
        }
        if (extraction.bureauScore() != null) {
            applied.append(" bureau=").append(extraction.bureauScore());
            application.setBureauScore(extraction.bureauScore());
        }
        applied.append(" (confidence ").append(extraction.confidence()).append(")");
        auditTrail.record(application.getId(), "EXTRACTED", ACTOR, applied.toString());
    }

    private void decide(LoanApplication application) {
        application.setStatus(ApplicationStatus.SCORING);
        RuleEngine.Evaluation evaluation = ruleEngine.evaluate(application);
        evaluation.outcomes().forEach(application::addRuleOutcome);

        application.setRiskScore(evaluation.score());
        application.setDecision(evaluation.decision());
        application.setApprovedAmount(approvedAmountFor(application, evaluation.decision()));
        application.setDecisionRationale(rationaleWriter.write(application, evaluation));
        application.setStatus(ApplicationStatus.DECIDED);
        application.setDecidedAt(Instant.now());

        auditTrail.record(
                application.getId(),
                "DECIDED",
                ACTOR,
                "%s at score %d. %s"
                        .formatted(evaluation.decision(), evaluation.score(), application.getDecisionRationale()));
    }

    /**
     * A REFER still carries a provisional amount so a human reviewer has a
     * starting point rather than an empty field.
     */
    private static BigDecimal approvedAmountFor(LoanApplication application, Decision decision) {
        return switch (decision) {
            case APPROVE -> application.getAmountRequested();
            case REFER -> application.getAmountRequested().multiply(BigDecimal.valueOf(0.6)).setScale(2, RoundingMode.HALF_UP);
            case DECLINE -> BigDecimal.ZERO;
        };
    }

    @Transactional(readOnly = true)
    public LoanApplication require(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<LoanApplication> forApplicant(String applicantRef) {
        return repository.findByApplicantRefOrderByCreatedAtDesc(applicantRef);
    }
}
