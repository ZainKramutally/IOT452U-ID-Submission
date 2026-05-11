package com.digitalid.verification;

import com.digitalid.domain.OrganisationType;

import java.time.LocalDate;

public record VerificationRequest(String digitalId, OrganisationType organisationType, LocalDate periodStart,
                                  LocalDate periodEnd) {
}