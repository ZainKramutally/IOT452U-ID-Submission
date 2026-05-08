package com.digitalid.verification;

public class VerificationResult {
    private final boolean exists;
    private final boolean valid;
    private final String reason;

    public VerificationResult(boolean exists, boolean valid, String reason) {
        this.exists = exists;
        this.valid = valid;
        this.reason = reason;
    }

    public boolean exists() {
        return exists;
    }

    public boolean isValid() {
        return valid;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "VerificationResult{" +
                "exists=" + exists +
                ", valid=" + valid +
                ", reason='" + reason + '\'' +
                '}';
    }
}

