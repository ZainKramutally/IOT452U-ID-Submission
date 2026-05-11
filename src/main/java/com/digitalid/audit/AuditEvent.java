package com.digitalid.audit;

import java.time.Instant;
import java.util.Objects;

public record AuditEvent(Instant timestamp, String action, String details) {
    public AuditEvent(Instant timestamp, String action, String details) {
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
        this.action = Objects.requireNonNull(action, "action");
        this.details = Objects.requireNonNull(details, "details");
    }
}