package com.digitalid.service;

import com.digitalid.audit.AuditActions;
import com.digitalid.audit.AuditLog;
import com.digitalid.domain.DigitalID;
import com.digitalid.domain.DigitalIDStatus;
import com.digitalid.domain.OrganisationType;
import com.digitalid.repository.IdentityRepository;

import java.time.LocalDate;
import java.util.Objects;

@SuppressWarnings("ClassCanBeRecord")
public class ManagementServiceImpl implements ManagementService {
    private final IdentityRepository repository;
    private final AuditLog auditLog;

    public ManagementServiceImpl(IdentityRepository repository, AuditLog auditLog) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.auditLog = Objects.requireNonNull(auditLog, "auditLog");
    }

    @Override
    public DigitalID createIdentity(String id, String fullName, LocalDate dateOfBirth, OrganisationType actor) {
        ensureCentralAuthority(actor, AuditActions.CREATE_IDENTITY, id);

        if (repository.exists(id)) {
            auditLog.record(AuditActions.rejected(AuditActions.CREATE_IDENTITY), "id=" + id + ",reason=DUPLICATE");
            throw new IllegalStateException("Digital ID already exists: " + id);
        }

        DigitalID digitalID = new DigitalID(id, fullName, dateOfBirth);
        repository.save(digitalID);
        auditLog.record(AuditActions.CREATE_IDENTITY, "id=" + id);
        return digitalID;
    }

    @Override
    public void updateName(String id, String fullName, OrganisationType actor) {
        ensureCentralAuthority(actor, AuditActions.UPDATE_NAME, id);
        DigitalID digitalID = loadIdentity(id);
        String previousName = digitalID.getFullName();

        if (digitalID.getStatus() == DigitalIDStatus.REVOKED) {
            auditLog.record(AuditActions.rejected(AuditActions.UPDATE_NAME), "id=" + id + ",reason=REVOKED");
            throw new IllegalStateException("Digital ID is revoked: " + digitalID.getId());
        }

        digitalID.updateFullName(fullName);
        repository.save(digitalID);
        auditLog.record(AuditActions.UPDATE_NAME, "id=" + id + ",from=" + previousName + ",to=" + fullName);
    }

    @Override
    public void changeStatus(String id, DigitalIDStatus newStatus, OrganisationType actor) {
        ensureCentralAuthority(actor, AuditActions.CHANGE_STATUS, id);
        DigitalID digitalID = loadIdentity(id);
        DigitalIDStatus previousStatus = digitalID.getStatus();

        if (previousStatus == newStatus) {
            auditLog.record(AuditActions.CHANGE_STATUS_NO_OP, "id=" + id + ",status=" + newStatus);
            return;
        }

        if (!previousStatus.canTransitionTo(newStatus)) {
            auditLog.record(AuditActions.rejected(AuditActions.CHANGE_STATUS),
                    "id=" + id + ",from=" + previousStatus + ",to=" + newStatus);
            throw new IllegalStateException(
                    "Invalid status transition from " + previousStatus + " to " + newStatus + " for id: " + id
            );
        }

        digitalID.recordStatusChange(newStatus);
        repository.save(digitalID);
        auditLog.record(AuditActions.CHANGE_STATUS, "id=" + id + ",from=" + previousStatus + ",to=" + newStatus);
    }

    @Override
    public void setRestricted(String id, boolean restricted, String reason, LocalDate expiresOn, OrganisationType actor) {
        ensureCentralAuthority(actor, AuditActions.SET_RESTRICTED, id);
        DigitalID digitalID = loadIdentity(id);

        if (digitalID.getStatus() == DigitalIDStatus.REVOKED) {
            auditLog.record(AuditActions.rejected(AuditActions.SET_RESTRICTED), "id=" + id + ",reason=REVOKED");
            throw new IllegalStateException("Digital ID is revoked: " + digitalID.getId());
        }

        String normalizedReason = normalizeReason(reason);
        LocalDate normalizedExpiry = null;
        if (restricted) {
            if (expiresOn == null) {
                throw new IllegalArgumentException("expiresOn must not be null when restricted");
            }
            normalizedExpiry = expiresOn;
        }

        digitalID.setRestricted(restricted, normalizedReason, normalizedExpiry);
        repository.save(digitalID);
        String details = "id=" + id + ",restricted=" + restricted + ",reason=" + normalizedReason;
        if (restricted) {
            details += ",expiresOn=" + normalizedExpiry;
        }
        auditLog.record(AuditActions.SET_RESTRICTED, details);
    }


    private DigitalID loadIdentity(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Digital ID not found: " + id));
    }

    private void ensureCentralAuthority(OrganisationType actor, String action, String id) {
        Objects.requireNonNull(actor, "actor");
        if (actor != OrganisationType.CENTRAL_AUTHORITY) {
            auditLog.record(AuditActions.rejected(action), "id=" + id + ",reason=UNAUTHORISED");
            throw new SecurityException("Only the central authority may perform this action");
        }
    }

    private static String normalizeReason(String reason) {
        Objects.requireNonNull(reason, "reason");
        String trimmed = reason.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
        return trimmed;
    }
}
