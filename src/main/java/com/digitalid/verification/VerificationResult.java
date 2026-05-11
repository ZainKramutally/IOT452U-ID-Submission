package com.digitalid.verification;

import com.digitalid.domain.ReasonCode;

import java.util.Objects;

public class VerificationResult {
    private final boolean exists;
    private final boolean valid;
    private final ReasonCode reason;
    private final String detail;

    public VerificationResult(boolean exists, boolean valid, ReasonCode reason, String detail) {
        this.exists = exists;
        this.valid = valid;
        this.reason = Objects.requireNonNull(reason, "reason");
        this.detail = detail; // null = no detail disclosed
    }

    public boolean exists() {
        return exists;
    }

    public boolean isValid() {
        return valid;
    }

    public ReasonCode getReason() {
        return reason;
    }

    public String getDetail() {
        return detail;
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
}