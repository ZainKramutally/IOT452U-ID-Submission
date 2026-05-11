package com.digitalid.service;

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
        ensureCentralAuthority(actor, "CREATE_IDENTITY", id);

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
    public void updateName(String id, String fullName, OrganisationType actor) {
        ensureCentralAuthority(actor, "UPDATE_NAME", id);
        DigitalID digitalID = loadIdentity(id);
        String previousName = digitalID.getFullName();

        if (digitalID.getStatus() == DigitalIDStatus.REVOKED) {
            auditLog.record("UPDATE_NAME_REJECTED", "id=" + id + ",reason=REVOKED");
            throw new IllegalStateException("Digital ID is revoked: " + digitalID.getId());
        }

        digitalID.updateFullName(fullName);
        repository.save(digitalID);
        auditLog.record("UPDATE_NAME", "id=" + id + ",from=" + previousName + ",to=" + fullName);
    }

    @Override
    public void changeStatus(String id, DigitalIDStatus newStatus, OrganisationType actor) {
        ensureCentralAuthority(actor, "CHANGE_STATUS", id);
        DigitalID digitalID = loadIdentity(id);
        DigitalIDStatus previousStatus = digitalID.getStatus();

        if (previousStatus == newStatus) {
            auditLog.record("CHANGE_STATUS_NO_OP", "id=" + id + ",status=" + newStatus);
            return;
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
    }

    @Override
    public void setRestricted(String id, boolean restricted, OrganisationType actor) {
        ensureCentralAuthority(actor, "SET_RESTRICTED", id);
        DigitalID digitalID = loadIdentity(id);

        if (digitalID.getStatus() == DigitalIDStatus.REVOKED) {
            auditLog.record("SET_RESTRICTED_REJECTED", "id=" + id + ",reason=REVOKED");
            throw new IllegalStateException("Digital ID is revoked: " + digitalID.getId());
        }

        digitalID.setRestricted(restricted);
        repository.save(digitalID);
        auditLog.record("SET_RESTRICTED", "id=" + id + ",restricted=" + restricted);
    }


    private DigitalID loadIdentity(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Digital ID not found: " + id));
    }

    private void ensureCentralAuthority(OrganisationType actor, String rejectedAction, String id) {
        Objects.requireNonNull(actor, "actor");
        if (actor != OrganisationType.CENTRAL_AUTHORITY) {
            auditLog.record(rejectedAction + "_REJECTED", "id=" + id + ",reason=UNAUTHORISED");
            throw new SecurityException("Only the central authority may perform this action");
        }
    }

}

