package com.digitalid.domain;

import java.time.Instant;
import java.util.Objects;

public record StatusChange(DigitalIDStatus status, Instant changedAt) {
    public StatusChange(DigitalIDStatus status, Instant changedAt) {
        this.status = Objects.requireNonNull(status, "status");
        this.changedAt = Objects.requireNonNull(changedAt, "changedAt");
    }
}