package com.digitalid.service;

import com.digitalid.audit.AuditLog;
import com.digitalid.domain.DigitalID;
import com.digitalid.domain.DigitalIDStatus;
import com.digitalid.domain.OrganisationType;
import com.digitalid.repository.InMemoryIdentityRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ManagementServiceTest {

    @Test
    void createsAndUpdatesIdentity() {
        InMemoryIdentityRepository repository = new InMemoryIdentityRepository();
        AuditLog auditLog = new AuditLog();
        ManagementService service = new ManagementServiceImpl(repository, auditLog);

        DigitalID created = service.createIdentity(
                "ID-100",
                "Taylor Quinn",
                LocalDate.of(1992, 3, 15),
                OrganisationType.CENTRAL_AUTHORITY
        );

        service.updateName(created.getId(), "Taylor Q.", OrganisationType.CENTRAL_AUTHORITY);
        service.changeStatus(created.getId(), DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);

        DigitalID stored = repository.findById(created.getId()).orElseThrow();
        assertEquals("Taylor Q.", stored.getFullName());
        assertEquals(DigitalIDStatus.SUSPENDED, stored.getStatus());
    }

    @Test
    void rejectsNonCentralAuthorityChanges() {
        InMemoryIdentityRepository repository = new InMemoryIdentityRepository();
        AuditLog auditLog = new AuditLog();
        ManagementService service = new ManagementServiceImpl(repository, auditLog);

        assertThrows(SecurityException.class, () -> service.createIdentity(
                "ID-200",
                "Avery Patel",
                LocalDate.of(1998, 9, 9),
                OrganisationType.EMPLOYER
        ));
    }
}

