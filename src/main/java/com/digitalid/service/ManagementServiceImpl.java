package com.digitalid.service;

import com.digitalid.audit.AuditLog;
import com.digitalid.domain.DigitalID;
import com.digitalid.domain.DigitalIDStatus;
import com.digitalid.domain.OrganisationType;
import com.digitalid.repository.IdentityRepository;

import java.time.LocalDate;
import java.util.Objects;

public class ManagementServiceImpl implements ManagementService {
    private final IdentityRepository repository;
    private final AuditLog auditLog;

    public ManagementServiceImpl(IdentityRepository repository, AuditLog auditLog) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.auditLog = Objects.requireNonNull(auditLog, "auditLog");
    }

    @Override
    public DigitalID createIdentity(String id, String fullName, LocalDate dateOfBirth, OrganisationType actor) {
        Objects.requireNonNull(actor, "actor");
        if (actor != OrganisationType.CENTRAL_AUTHORITY) {
            auditLog.record("CREATE_IDENTITY_REJECTED", "id=" + id + ",reason=UNAUTHORISED");
            throw new SecurityException("Only the central authority may perform this action");
        }

        if (repository.exists(id)) {
            auditLog.record("CREATE_IDENTITY_REJECTED", "id=" + id + ",reason=DUPLICATE");
            throw new IllegalStateException("Digital ID already exists: " + id);
        }

        DigitalID digitalID = new DigitalID(id, fullName, dateOfBirth);
        repository.save(digitalID);
        auditLog.record("CREATE_IDENTITY", "id=" + id);
        return digitalID;
    }

    @Override
    public DigitalID updateName(String id, String fullName, OrganisationType actor) {
        ensureCentralAuthority(actor);
        DigitalID digitalID = loadIdentity(id);
        String previousName = digitalID.getFullName(); // capture before change

        if (digitalID.getStatus() == DigitalIDStatus.REVOKED) {
            auditLog.record("UPDATE_NAME_REJECTED", "id=" + id + ",reason=REVOKED");
            throw new IllegalStateException("Digital ID is revoked: " + digitalID.getId());
        }

        digitalID.updateFullName(fullName);
        repository.save(digitalID);
        auditLog.record("UPDATE_NAME", "id=" + id + ",from=" + previousName + ",to=" + fullName);
        return digitalID;
    }

    @Override
    public DigitalID changeStatus(String id, DigitalIDStatus newStatus, OrganisationType actor) {
        ensureCentralAuthority(actor);
        DigitalID digitalID = loadIdentity(id);
        DigitalIDStatus previousStatus = digitalID.getStatus(); // capture before any change

        if (previousStatus == newStatus) {
            auditLog.record("CHANGE_STATUS_NO_OP", "id=" + id + ",status=" + newStatus);
            return digitalID;
        }

        if (!previousStatus.canTransitionTo(newStatus)) {
            auditLog.record("CHANGE_STATUS_REJECTED", "id=" + id + ",from=" + previousStatus + ",to=" + newStatus);
            throw new IllegalStateException(
                    "Invalid status transition from " + previousStatus + " to " + newStatus + " for id: " + id
            );
        }

        digitalID.recordStatusChange(newStatus);
        repository.save(digitalID);
        auditLog.record("CHANGE_STATUS", "id=" + id + ",from=" + previousStatus + ",to=" + newStatus);
        return digitalID;
    }

    @Override
    public DigitalID setRestricted(String id, boolean restricted, OrganisationType actor) {
        ensureCentralAuthority(actor);
        DigitalID digitalID = loadIdentity(id);

        if (digitalID.getStatus() == DigitalIDStatus.REVOKED) {
            auditLog.record("SET_RESTRICTED_REJECTED", "id=" + id + ",reason=REVOKED");
            throw new IllegalStateException("Digital ID is revoked: " + digitalID.getId());
        }

        digitalID.setRestricted(restricted);
        repository.save(digitalID);
        auditLog.record("SET_RESTRICTED", "id=" + id + ",restricted=" + restricted);
        return digitalID;
    }

    private DigitalID loadIdentity(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Digital ID not found: " + id));
    }

    private void ensureCentralAuthority(OrganisationType actor) {
        Objects.requireNonNull(actor, "actor");
        if (actor != OrganisationType.CENTRAL_AUTHORITY) {
            throw new SecurityException("Only the central authority may perform this action");
        }
    }

}

