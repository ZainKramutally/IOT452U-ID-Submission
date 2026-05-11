package com.digitalid.audit;

import java.time.Instant;
import java.util.Objects;

public record AuditEvent(Instant timestamp, String action, String details) {
    public AuditEvent {
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(details, "details");
    }
}