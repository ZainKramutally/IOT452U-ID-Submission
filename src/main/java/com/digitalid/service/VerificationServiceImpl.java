package com.digitalid.service;

import com.digitalid.audit.AuditLog;
import com.digitalid.domain.DigitalID;
import com.digitalid.domain.DigitalIDStatus;
import com.digitalid.domain.OrganisationType;
import com.digitalid.repository.IdentityRepository;
import com.digitalid.verification.VerificationRequest;
import com.digitalid.verification.VerificationResult;

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
                .map(identity -> evaluate(identity, request.getOrganisationType()))
                .orElseGet(() -> {
                    auditLog.record("VERIFY_NOT_FOUND", "id=" + request.getDigitalId());
                    return new VerificationResult(false, false, "NOT_FOUND");
                });
    }

    private VerificationResult evaluate(DigitalID identity, OrganisationType organisationType) {
        boolean active = identity.getStatus() == DigitalIDStatus.ACTIVE;
        boolean valid;
        String reason;

        if (!active) {
            valid = false;
            reason = "INACTIVE";
        } else if (organisationType == OrganisationType.DRIVING_LICENCE_AUTHORITY && identity.isRestricted()) {
            valid = false;
            reason = "RESTRICTED";
        } else {
            valid = true;
            reason = "VALID";
        }

        auditLog.record("VERIFY", "id=" + identity.getId() + ",org=" + organisationType + ",result=" + reason);
        return new VerificationResult(true, valid, reason);
    }
}

