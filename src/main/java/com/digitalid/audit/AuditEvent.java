package com.digitalid.audit;

import java.time.Instant;
import java.util.Objects;

public class AuditEvent {
    private final Instant timestamp;
    private final String action;
    private final String details;

    public AuditEvent(Instant timestamp, String action, String details) {
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
        this.action = Objects.requireNonNull(action, "action");
        this.details = Objects.requireNonNull(details, "details");
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }
}