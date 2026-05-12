package com.digitalid.audit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AuditLog implements AuditRecorder {
    private final List<AuditEvent> events = new ArrayList<>();

    public void record(String action, String details) {
        events.add(new AuditEvent(Instant.now(), action, details));
    }

    public List<AuditEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public void printAll() {
        events.forEach(e ->
                System.out.println("[" + e.timestamp() + "] " + e.action() + " | " + e.details())
        );
    }
}