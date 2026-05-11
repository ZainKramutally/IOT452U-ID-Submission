package com.digitalid.service;

import com.digitalid.audit.AuditLog;
import com.digitalid.domain.DigitalID;
import com.digitalid.domain.DigitalIDStatus;
import com.digitalid.domain.OrganisationType;
import com.digitalid.domain.ReasonCode;
import com.digitalid.domain.StatusChange;
import com.digitalid.repository.IdentityRepository;
import com.digitalid.verification.VerificationRequest;
import com.digitalid.verification.VerificationResult;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;

public class VerificationServiceImpl implements VerificationService {
    private final IdentityRepository repository;
    private final AuditLog auditLog;

    public VerificationServiceImpl(IdentityRepository repository, AuditLog auditLog) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.auditLog = Objects.requireNonNull(auditLog, "auditLog");
    }

    @Override
    public VerificationResult verify(VerificationRequest request) {
        Objects.requireNonNull(request, "request");

        return repository.findById(request.getDigitalId())
                .map(identity -> evaluate(identity, request))
                .orElseGet(() -> {
                    auditLog.record("VERIFY_NOT_FOUND", "id=" + request.getDigitalId()
                            + ",org=" + request.getOrganisationType());
                    return new VerificationResult(false, false, ReasonCode.NOT_FOUND, null);
                });
    }

    private VerificationResult evaluate(DigitalID identity, VerificationRequest request) {
        OrganisationType orgType = request.getOrganisationType();

        VerificationResult result = switch (orgType) {
            case TAX_AUTHORITY -> evaluateForTaxAuthority(identity, request.getPeriodStart(), request.getPeriodEnd());
            case DRIVING_LICENCE_AUTHORITY -> evaluateForDrivingLicence(identity);
            case EMPLOYER, BANK -> evaluateForEmployerOrBank(identity);
            case CENTRAL_AUTHORITY -> throw new SecurityException(
                    "Central authority is not permitted to perform verification requests"
            );
        };

        auditLog.record("VERIFY", "id=" + identity.getId()
                + ",org=" + orgType
                + ",result=" + result.getReason());

        return result;
    }

    private VerificationResult evaluateForTaxAuthority(DigitalID identity,
                                                       LocalDate periodStart,
                                                       LocalDate periodEnd) {
        if (periodStart == null || periodEnd == null) {
            throw new IllegalArgumentException(
                    "Tax authority verification requires both periodStart and periodEnd"
            );
        }
        if (periodStart.isAfter(periodEnd)) {
            throw new IllegalArgumentException(
                    "Tax authority verification requires periodStart to be on or before periodEnd"
            );
        }

        boolean suspendedDuringPeriod = identity.getStatusHistory().stream()
                .filter(sc -> sc.status() == DigitalIDStatus.SUSPENDED)
                .anyMatch(sc -> isBetween(sc, periodStart, periodEnd));

        if (suspendedDuringPeriod) {
            return new VerificationResult(true, false, ReasonCode.SUSPENDED_DURING_PERIOD, null);
        }

        if (identity.getStatus() != DigitalIDStatus.ACTIVE) {
            return new VerificationResult(true, false, ReasonCode.INACTIVE, null);
        }

        return new VerificationResult(true, true, ReasonCode.VALID, null);
    }

    private VerificationResult evaluateForDrivingLicence(DigitalID identity) {
        if (identity.getStatus() != DigitalIDStatus.ACTIVE) {
            return new VerificationResult(true, false, ReasonCode.INACTIVE, null);
        }

        if (identity.isRestricted()) {
            return new VerificationResult(true, false, ReasonCode.RESTRICTED, null);
        }

        return new VerificationResult(true, true, ReasonCode.VALID, null);
    }

    // No reason detail is exposed to enforce limited disclosure in code
    private VerificationResult evaluateForEmployerOrBank(DigitalID identity) {
        boolean active = identity.getStatus() == DigitalIDStatus.ACTIVE;
        return new VerificationResult(true, active, active ? ReasonCode.VALID : ReasonCode.INACTIVE, null);
    }

    private boolean isBetween(StatusChange sc, LocalDate periodStart, LocalDate periodEnd) {
        LocalDate changeDate = sc.changedAt().atZone(ZoneOffset.UTC).toLocalDate();
        return !changeDate.isBefore(periodStart) && !changeDate.isAfter(periodEnd);
    }
}