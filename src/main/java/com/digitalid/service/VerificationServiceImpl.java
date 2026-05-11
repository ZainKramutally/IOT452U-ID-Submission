package com.digitalid.service;

import com.digitalid.audit.AuditActions;
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

@SuppressWarnings("ClassCanBeRecord")
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

        return repository.findById(request.digitalId())
                .map(identity -> evaluate(identity, request))
                .orElseGet(() -> {
                    auditLog.record(AuditActions.VERIFY_NOT_FOUND, "id=" + request.digitalId()
                            + ",org=" + request.organisationType());
                    return new VerificationResult(false, false, ReasonCode.NOT_FOUND, null);
                });
    }

    private VerificationResult evaluate(DigitalID identity, VerificationRequest request) {
        OrganisationType orgType = request.organisationType();

        VerificationResult result = switch (orgType) {
            case TAX_AUTHORITY -> evaluateForTaxAuthority(identity, request.periodStart(), request.periodEnd());
            case DRIVING_LICENCE_AUTHORITY -> evaluateForDrivingLicence(identity);
            case EMPLOYER, BANK -> evaluateForEmployerOrBank(identity);
            case CENTRAL_AUTHORITY -> throw new SecurityException(
                    "Central authority is not permitted to perform verification requests"
            );
        };

        auditLog.record(AuditActions.VERIFY, "id=" + identity.getId()
                + ",org=" + orgType
                + ",result=" + result.reason());

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

        boolean suspendedDuringPeriod = wasSuspendedDuringPeriod(identity, periodStart, periodEnd);

        if (suspendedDuringPeriod) {
            return new VerificationResult(true, false, ReasonCode.SUSPENDED_DURING_PERIOD, null);
        }

        if (identity.getStatus() != DigitalIDStatus.ACTIVE) {
            return new VerificationResult(true, false, ReasonCode.INACTIVE, null);
        }

        return new VerificationResult(true, true, ReasonCode.VALID, null);
    }

    private boolean wasSuspendedDuringPeriod(DigitalID identity,
                                             LocalDate periodStart,
                                             LocalDate periodEnd) {
        var history = identity.getStatusHistory();

        for (int i = 0; i < history.size(); i++) {
            StatusChange current = history.get(i);

            if (current.status() != DigitalIDStatus.SUSPENDED) {
                continue;
            }

            LocalDate suspensionStart = current.changedAt().atZone(ZoneOffset.UTC).toLocalDate();
            LocalDate suspensionEnd = (i + 1 < history.size())
                    ? history.get(i + 1).changedAt().atZone(ZoneOffset.UTC).toLocalDate()
                    : null; // still suspended with no end date

            boolean overlaps = overlapsPeriod(suspensionStart, suspensionEnd, periodStart, periodEnd);

            if (overlaps) {
                return true;
            }
        }

        return false;
    }

    private boolean overlapsPeriod(LocalDate rangeStart,
                                   LocalDate rangeEnd,
                                   LocalDate periodStart,
                                   LocalDate periodEnd) {
        return !rangeStart.isAfter(periodEnd)
                && (rangeEnd == null || !rangeEnd.isBefore(periodStart));
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
        return new VerificationResult(true, active, active ? ReasonCode.VALID : ReasonCode.INVALID, null);
    }

}