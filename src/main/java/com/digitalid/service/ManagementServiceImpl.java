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
        ensureCentralAuthority(actor);
        if (repository.exists(id)) {
            throw new IllegalStateException("Digital ID already exists: " + id);
        }
        DigitalID digitalID = new DigitalID(id, fullName, dateOfBirth);
        repository.save(digitalID);
        auditLog.record("CREATE_ID", "id=" + id);
        return digitalID;
    }

    @Override
    public DigitalID updateName(String id, String fullName, OrganisationType actor) {
        ensureCentralAuthority(actor);
        DigitalID digitalID = loadIdentity(id);
        ensureNotRevoked(digitalID);
        digitalID.updateFullName(fullName);
        repository.save(digitalID);
        auditLog.record("UPDATE_NAME", "id=" + id);
        return digitalID;
    }

    @Override
    public DigitalID changeStatus(String id, DigitalIDStatus status, OrganisationType actor) {
        ensureCentralAuthority(actor);
        DigitalID digitalID = loadIdentity(id);
        if (digitalID.getStatus() == DigitalIDStatus.REVOKED && status != DigitalIDStatus.REVOKED) {
            throw new IllegalStateException("Cannot change status after revocation: " + id);
        }
        digitalID.recordStatusChange(status);
        repository.save(digitalID);
        auditLog.record("CHANGE_STATUS", "id=" + id + ",status=" + status);
        return digitalID;
    }

    @Override
    public DigitalID setRestricted(String id, boolean restricted, OrganisationType actor) {
        ensureCentralAuthority(actor);
        DigitalID digitalID = loadIdentity(id);
        ensureNotRevoked(digitalID);
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
        if (actor != OrganisationType.CENTRAL_AUTHORITY) {
            throw new SecurityException("Only the central authority may perform this action");
        }
    }

    private void ensureNotRevoked(DigitalID digitalID) {
        if (digitalID.getStatus() == DigitalIDStatus.REVOKED) {
            throw new IllegalStateException("Digital ID is revoked: " + digitalID.getId());
        }
    }
}

