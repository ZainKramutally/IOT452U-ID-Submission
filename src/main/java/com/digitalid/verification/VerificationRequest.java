package com.digitalid.verification;

import com.digitalid.domain.OrganisationType;

import java.time.LocalDate;
import java.util.Objects;

public class VerificationRequest {
    private final String digitalId;
    private final OrganisationType organisationType;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;

    public VerificationRequest(String digitalId, OrganisationType organisationType,
                               LocalDate periodStart, LocalDate periodEnd) {
        this.digitalId = Objects.requireNonNull(digitalId, "digitalId");
        this.organisationType = Objects.requireNonNull(organisationType, "organisationType");
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    public String getDigitalId() {
        return digitalId;
    }

    public OrganisationType getOrganisationType() {
        return organisationType;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }
}