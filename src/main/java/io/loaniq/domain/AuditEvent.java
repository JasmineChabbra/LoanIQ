package io.loaniq.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Append-only record of everything that happened to an application. */
@Entity
@Table(
        name = "audit_event",
        indexes = @Index(name = "idx_audit_event_application", columnList = "application_id, occurred_at"))
public class AuditEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "actor", nullable = false, length = 64)
    private String actor;

    @Column(name = "detail", nullable = false, length = 4000)
    private String detail;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();

    protected AuditEvent() {
        // for JPA
    }

    public AuditEvent(UUID applicationId, String eventType, String actor, String detail) {
        this.applicationId = applicationId;
        this.eventType = eventType;
        this.actor = actor;
        this.detail = detail;
    }

    public UUID getId() {
        return id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getActor() {
        return actor;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
