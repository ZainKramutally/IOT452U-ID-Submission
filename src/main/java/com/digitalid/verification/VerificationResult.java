package com.digitalid.verification;

import com.digitalid.domain.ReasonCode;

import java.util.Objects;

public record VerificationResult(boolean exists, boolean valid, ReasonCode reason, String detail) {
    public VerificationResult(boolean exists, boolean valid, ReasonCode reason, String detail) {
        this.exists = exists;
        this.valid = valid;
        this.reason = Objects.requireNonNull(reason, "reason");
        this.detail = detail; // null = no detail disclosed
    }

    @Override
    public String toString() {
        return "VerificationResult{" +
                "exists=" + exists +
                ", valid=" + valid +
                ", reason=" + reason +
                ", detail='" + detail + '\'' +
                '}';
    }

    public String getDetail() {
        return detail;
    }

    public String getReason() {
        return reason.name();
    }

    public String isValid() {
        return Boolean.toString(valid);
    }
}