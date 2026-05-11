package com.digitalid.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record RestrictionChange(boolean restricted, String reason, LocalDate expiresOn, Instant changedAt) {
    public RestrictionChange {
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(changedAt, "changedAt");
    }
}

