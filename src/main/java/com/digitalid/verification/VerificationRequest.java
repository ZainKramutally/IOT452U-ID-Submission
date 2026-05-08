package com.digitalid.verification;

import com.digitalid.domain.OrganisationType;

import java.time.LocalDate;
import java.util.Objects;

public class VerificationRequest {
    private final String digitalId;
    private final OrganisationType organisationType;
    private final LocalDate onDate;

    public VerificationRequest(String digitalId, OrganisationType organisationType, LocalDate onDate) {
        this.digitalId = Objects.requireNonNull(digitalId, "digitalId");
        this.organisationType = Objects.requireNonNull(organisationType, "organisationType");
        this.onDate = Objects.requireNonNull(onDate, "onDate");
    }

    public String getDigitalId() {
        return digitalId;
    }

    public OrganisationType getOrganisationType() {
        return organisationType;
    }

}

