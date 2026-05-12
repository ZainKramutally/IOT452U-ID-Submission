package com.digitalid.audit;

public interface AuditRecorder {
    void record(String action, String details);
}

