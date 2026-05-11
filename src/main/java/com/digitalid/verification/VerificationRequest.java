package com.digitalid.verification;

import com.digitalid.domain.OrganisationType;

import java.time.LocalDate;
import java.util.Objects;

public record VerificationRequest(String digitalId, OrganisationType organisationType, LocalDate periodStart,
                                  LocalDate periodEnd) {
    public VerificationRequest(String digitalId, OrganisationType organisationType,
                               LocalDate periodStart, LocalDate periodEnd) {
        this.digitalId = Objects.requireNonNull(digitalId, "digitalId");
        this.organisationType = Objects.requireNonNull(organisationType, "organisationType");
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }
}