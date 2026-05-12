package com.digitalid.service;

import com.digitalid.audit.AuditActions;
import com.digitalid.audit.AuditDetailKeys;
import com.digitalid.audit.AuditDetails;
import com.digitalid.audit.AuditRecorder;
import com.digitalid.audit.AuditReasons;
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
    private final AuditRecorder auditLog;

    public VerificationServiceImpl(IdentityRepository repository, AuditRecorder auditLog) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.auditLog = Objects.requireNonNull(auditLog, "auditLog");
    }

    @Override
    public VerificationResult verify(VerificationRequest request) {
        if (request == null) {
            recordRejection(AuditActions.VERIFY, null, null, AuditReasons.MISSING_REQUEST);
            throw new NullPointerException("request");
        }

        OrganisationType orgType = requireOrganisationType(request.organisationType());

        if (orgType == OrganisationType.CENTRAL_AUTHORITY) {
            recordRejection(AuditActions.VERIFY, request.digitalId(), orgType, AuditReasons.UNAUTHORISED);
            throw new SecurityException("Central authority is not permitted to perform verification requests");
        }

        String digitalId = requireText(request.digitalId(), "digitalId", orgType, AuditReasons.MISSING_ID);

        return repository.findById(digitalId)
                .map(identity -> evaluate(identity, request))
                .orElseGet(() -> {
                    auditLog.record(AuditActions.VERIFY_NOT_FOUND, AuditDetails.details(
                            AuditDetails.detail(AuditDetailKeys.ID, digitalId),
                            AuditDetails.detail(AuditDetailKeys.ORG, orgType)
                    ));
                    return new VerificationResult(false, false, ReasonCode.NOT_FOUND, null);
                });
    }

    private VerificationResult evaluate(DigitalID identity, VerificationRequest request) {
        OrganisationType orgType = request.organisationType();

        VerificationResult result = switch (orgType) {
            case TAX_AUTHORITY -> evaluateForTaxAuthority(identity, request.periodStart(), request.periodEnd());
            case DRIVING_LICENCE_AUTHORITY -> evaluateForDrivingLicence(identity);
            case EMPLOYER, BANK -> evaluateForEmployerOrBank(identity);
            case CENTRAL_AUTHORITY -> {
                // Defensive check: central authority requests should be rejected before lookup.
                recordRejection(AuditActions.VERIFY, identity.getId(), orgType, AuditReasons.UNAUTHORISED);
                throw new SecurityException(
                        "Central authority is not permitted to perform verification requests"
                );
            }
        };

        auditLog.record(AuditActions.VERIFY, AuditDetails.details(
                AuditDetails.detail(AuditDetailKeys.ID, identity.getId()),
                AuditDetails.detail(AuditDetailKeys.ORG, orgType),
                AuditDetails.detail(AuditDetailKeys.RESULT, result.reason())
        ));

        return result;
    }

    private VerificationResult evaluateForTaxAuthority(DigitalID identity,
                                                       LocalDate periodStart,
                                                       LocalDate periodEnd) {
        if (periodStart == null || periodEnd == null) {
            String reason = (periodStart == null && periodEnd == null)
                    ? AuditReasons.MISSING_PERIODS
                    : (periodStart == null ? AuditReasons.MISSING_PERIOD_START : AuditReasons.MISSING_PERIOD_END);
            recordRejection(AuditActions.VERIFY, identity.getId(), OrganisationType.TAX_AUTHORITY, reason);
            throw new IllegalArgumentException(
                    "Tax authority verification requires both periodStart and periodEnd"
            );
        }

        if (periodStart.isAfter(periodEnd)) {
            recordRejection(AuditActions.VERIFY, identity.getId(), OrganisationType.TAX_AUTHORITY,
                    AuditReasons.INVALID_PERIOD_RANGE);
            throw new IllegalArgumentException("periodStart must be on or before periodEnd");
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
        return new VerificationResult(true, active, active ? ReasonCode.VALID : ReasonCode.INACTIVE, null);
    }

    private OrganisationType requireOrganisationType(OrganisationType orgType) {
        if (orgType == null) {
            recordRejection(AuditActions.VERIFY, null, null, AuditReasons.MISSING_ORG);
            throw new NullPointerException("organisationType");
        }
        return orgType;
    }

    private String requireText(String value, String field, OrganisationType orgType, String missingReason) {
        if (value == null) {
            recordRejection(AuditActions.VERIFY, null, orgType, missingReason);
            throw new NullPointerException(field);
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            recordRejection(AuditActions.VERIFY, value, orgType, missingReason);
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return trimmed;
    }

    private void recordRejection(String action, String id, OrganisationType orgType, String reason) {
        auditLog.record(AuditActions.rejected(action), AuditDetails.details(
                AuditDetails.detail(AuditDetailKeys.ID, id),
                AuditDetails.detail(AuditDetailKeys.ORG, orgType),
                AuditDetails.detail(AuditDetailKeys.REASON, reason)
        ));
    }
}

