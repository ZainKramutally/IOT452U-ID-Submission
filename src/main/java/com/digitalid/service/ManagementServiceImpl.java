package com.digitalid.service;

import com.digitalid.audit.AuditActions;
import com.digitalid.audit.AuditDetailKeys;
import com.digitalid.audit.AuditLog;
import com.digitalid.audit.AuditReasons;
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
        String checkedId = requireText(id, "id", AuditActions.CREATE_IDENTITY, id, AuditReasons.MISSING_ID);
        String checkedFullName = requireText(fullName, "fullName", AuditActions.CREATE_IDENTITY, checkedId,
                AuditReasons.MISSING_FULL_NAME);
        LocalDate checkedDob = requireDate(dateOfBirth, "dateOfBirth", AuditActions.CREATE_IDENTITY, checkedId,
                AuditReasons.MISSING_DATE_OF_BIRTH);

        if (repository.exists(checkedId)) {
            recordRejection(AuditActions.CREATE_IDENTITY, checkedId, AuditReasons.DUPLICATE);
            throw new IllegalStateException("Digital ID already exists: " + checkedId);
        }

        DigitalID digitalID = new DigitalID(checkedId, checkedFullName, checkedDob);
        repository.save(digitalID);
        auditLog.record(AuditActions.CREATE_IDENTITY, details(detail(AuditDetailKeys.ID, checkedId)));
        return digitalID;
    }

    @Override
    public void updateName(String id, String fullName, OrganisationType actor) {
        ensureCentralAuthority(actor, AuditActions.UPDATE_NAME, id);
        String checkedId = requireText(id, "id", AuditActions.UPDATE_NAME, id, AuditReasons.MISSING_ID);
        String checkedFullName = requireText(fullName, "fullName", AuditActions.UPDATE_NAME, checkedId,
                AuditReasons.MISSING_FULL_NAME);
        DigitalID digitalID = loadIdentity(checkedId, AuditActions.UPDATE_NAME);
        String previousName = digitalID.getFullName();

        if (digitalID.getStatus() == DigitalIDStatus.REVOKED) {
            recordRejection(AuditActions.UPDATE_NAME, checkedId, AuditReasons.REVOKED);
            throw new IllegalStateException("Digital ID is revoked: " + digitalID.getId());
        }

        digitalID.updateFullName(checkedFullName);
        repository.save(digitalID);
        auditLog.record(AuditActions.UPDATE_NAME, details(
                detail(AuditDetailKeys.ID, checkedId),
                detail(AuditDetailKeys.FROM, previousName),
                detail(AuditDetailKeys.TO, checkedFullName)
        ));
    }

    @Override
    public void changeStatus(String id, DigitalIDStatus newStatus, OrganisationType actor) {
        ensureCentralAuthority(actor, AuditActions.CHANGE_STATUS, id);
        String checkedId = requireText(id, "id", AuditActions.CHANGE_STATUS, id, AuditReasons.MISSING_ID);
        DigitalIDStatus checkedStatus = requireStatus(newStatus, AuditActions.CHANGE_STATUS, checkedId);
        DigitalID digitalID = loadIdentity(checkedId, AuditActions.CHANGE_STATUS);
        DigitalIDStatus previousStatus = digitalID.getStatus();

        if (previousStatus == checkedStatus) {
            auditLog.record(AuditActions.CHANGE_STATUS_NO_OP, details(
                    detail(AuditDetailKeys.ID, checkedId),
                    detail(AuditDetailKeys.STATUS, checkedStatus)
            ));
            return;
        }

        if (!previousStatus.canTransitionTo(checkedStatus)) {
            auditLog.record(AuditActions.rejected(AuditActions.CHANGE_STATUS), details(
                    detail(AuditDetailKeys.ID, checkedId),
                    detail(AuditDetailKeys.FROM, previousStatus),
                    detail(AuditDetailKeys.TO, checkedStatus)
            ));
            throw new IllegalStateException(
                    "Invalid status transition from " + previousStatus + " to " + checkedStatus + " for id: " + checkedId
            );
        }

        digitalID.recordStatusChange(checkedStatus);
        repository.save(digitalID);
        auditLog.record(AuditActions.CHANGE_STATUS, details(
                detail(AuditDetailKeys.ID, checkedId),
                detail(AuditDetailKeys.FROM, previousStatus),
                detail(AuditDetailKeys.TO, checkedStatus)
        ));
    }

    @Override
    public void setRestricted(String id, boolean restricted, String reason, LocalDate expiresOn, OrganisationType actor) {
        ensureCentralAuthority(actor, AuditActions.SET_RESTRICTED, id);
        String checkedId = requireText(id, "id", AuditActions.SET_RESTRICTED, id, AuditReasons.MISSING_ID);
        DigitalID digitalID = loadIdentity(checkedId, AuditActions.SET_RESTRICTED);

        if (digitalID.getStatus() == DigitalIDStatus.REVOKED) {
            recordRejection(AuditActions.SET_RESTRICTED, checkedId, AuditReasons.REVOKED);
            throw new IllegalStateException("Digital ID is revoked: " + digitalID.getId());
        }

        String normalizedReason = requireText(reason, "reason", AuditActions.SET_RESTRICTED, checkedId,
                AuditReasons.MISSING_REASON);
        LocalDate normalizedExpiry = restricted ? expiresOn : null;

        digitalID.setRestricted(restricted, normalizedReason, normalizedExpiry);
        repository.save(digitalID);
        String details = details(
                detail(AuditDetailKeys.ID, checkedId),
                detail(AuditDetailKeys.RESTRICTED, restricted),
                detail(AuditDetailKeys.REASON, normalizedReason)
        );
        if (normalizedExpiry != null) {
            details += "," + detail(AuditDetailKeys.EXPIRES_ON, normalizedExpiry);
        }
        auditLog.record(AuditActions.SET_RESTRICTED, details);
    }

    private DigitalID loadIdentity(String id, String action) {
        return repository.findById(id)
                .orElseThrow(() -> {
                    recordRejection(action, id, AuditReasons.NOT_FOUND);
                    return new IllegalArgumentException("Digital ID not found: " + id);
                });
    }

    private void ensureCentralAuthority(OrganisationType actor, String action, String id) {
        if (actor == null) {
            recordRejection(action, id, AuditReasons.MISSING_ACTOR);
            throw new NullPointerException("actor");
        }
        if (actor != OrganisationType.CENTRAL_AUTHORITY) {
            recordRejection(action, id, AuditReasons.UNAUTHORISED);
            throw new SecurityException("Only the central authority may perform this action");
        }
    }

    private String requireText(String value, String field, String action, String id, String missingReason) {
        if (value == null) {
            recordRejection(action, id, missingReason);
            throw new NullPointerException(field);
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            recordRejection(action, id, missingReason);
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return trimmed;
    }

    private LocalDate requireDate(LocalDate value, String field, String action, String id, String missingReason) {
        if (value == null) {
            recordRejection(action, id, missingReason);
            throw new NullPointerException(field);
        }
        return value;
    }

    private DigitalIDStatus requireStatus(DigitalIDStatus status, String action, String id) {
        if (status == null) {
            recordRejection(action, id, AuditReasons.MISSING_STATUS);
            throw new NullPointerException("status");
        }
        return status;
    }

    private void recordRejection(String action, String id, String reason) {
        auditLog.record(AuditActions.rejected(action), details(
                detail(AuditDetailKeys.ID, id),
                detail(AuditDetailKeys.REASON, reason)
        ));
    }

    private static String detail(String key, Object value) {
        return key + "=" + value;
    }

    private static String details(String... parts) {
        return String.join(",", parts);
    }
}
