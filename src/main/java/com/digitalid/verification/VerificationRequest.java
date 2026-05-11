package com.digitalid.verification;

import com.digitalid.domain.OrganisationType;

import java.time.LocalDate;

public record VerificationRequest(String digitalId, OrganisationType organisationType, LocalDate periodStart,
                                  LocalDate periodEnd) {
    public VerificationRequest {
        java.util.Objects.requireNonNull(digitalId, "digitalId");
        java.util.Objects.requireNonNull(organisationType, "organisationType");
        String trimmedId = digitalId.trim();
        if (trimmedId.isEmpty()) {
            throw new IllegalArgumentException("digitalId must not be blank");
        }
        digitalId = trimmedId;
    }
}