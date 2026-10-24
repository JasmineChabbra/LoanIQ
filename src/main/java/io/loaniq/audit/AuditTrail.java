package io.loaniq.audit;

import io.loaniq.domain.AuditEvent;
import io.loaniq.repo.AuditEventRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Append-only history of everything that touched an application.
 *
 * <p>Writes run in their own transaction. If the underwriting transaction
 * later rolls back, the trail still records that the attempt happened, which
 * is exactly what an investigator needs when something fails halfway.
 */
@Service
public class AuditTrail {

    private final AuditEventRepository repository;

    public AuditTrail(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID applicationId, String eventType, String actor, String detail) {
        repository.save(new AuditEvent(applicationId, eventType, actor, truncate(detail)));
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> forApplication(UUID applicationId) {
        return repository.findByApplicationIdOrderByOccurredAtAsc(applicationId);
    }

    private static String truncate(String detail) {
        if (detail == null) {
            return "";
        }
        return detail.length() <= 4000 ? detail : detail.substring(0, 3997) + "...";
    }
}
