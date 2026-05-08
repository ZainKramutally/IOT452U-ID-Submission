package com.digitalid.audit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AuditLog {
    private final List<AuditEvent> events = new ArrayList<>();

    public void record(String action, String details) {
        events.add(new AuditEvent(Instant.now(), action, details));
    }
}

