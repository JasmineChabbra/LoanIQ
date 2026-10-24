package io.loaniq.api;

import io.loaniq.audit.AuditTrail;
import io.loaniq.domain.AuditEvent;
import io.loaniq.domain.LoanApplication;
import io.loaniq.service.UnderwritingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/applications")
@Tag(name = "Underwriting", description = "Submit credit applications and read their decisions")
public class UnderwritingController {

    private final UnderwritingService service;
    private final AuditTrail auditTrail;

    public UnderwritingController(UnderwritingService service, AuditTrail auditTrail) {
        this.service = service;
        this.auditTrail = auditTrail;
    }

    @PostMapping
    @Operation(
            summary = "Submit an application for underwriting",
            description =
                    """
                    The Idempotency-Key header makes submission safe to retry: the same key
                    always returns the original decision instead of scoring again.
                    """)
    public ResponseEntity<ApplicationView> submit(
            @Parameter(description = "Client-generated key, unique per application attempt")
                    @RequestHeader("Idempotency-Key")
                    String idempotencyKey,
            @Valid @RequestBody SubmitApplicationRequest request) {
        LoanApplication application = service.submit(idempotencyKey, request);
        return ResponseEntity.created(URI.create("/api/v1/applications/" + application.getId()))
                .body(ApplicationView.from(application));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch one application with its rule outcomes")
    public ApplicationView get(@PathVariable UUID id) {
        return ApplicationView.from(service.require(id));
    }

    @GetMapping
    @Operation(summary = "List an applicant's applications, newest first")
    public List<ApplicationView> byApplicant(@RequestParam String applicantRef) {
        return service.forApplicant(applicantRef).stream()
                .map(ApplicationView::from)
                .toList();
    }

    @GetMapping("/{id}/audit")
    @Operation(summary = "Replay the full audit trail for an application")
    public List<AuditEvent> audit(@PathVariable UUID id) {
        service.require(id);
        return auditTrail.forApplication(id);
    }
}
