package com.digitalid.service;

import com.digitalid.audit.AuditLog;
import com.digitalid.domain.OrganisationType;
import com.digitalid.repository.InMemoryIdentityRepository;
import com.digitalid.verification.VerificationRequest;
import com.digitalid.verification.VerificationResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificationServiceTest {

    @Test
    void verifiesActiveIdentityForEmployer() {
        InMemoryIdentityRepository repository = new InMemoryIdentityRepository();
        AuditLog auditLog = new AuditLog();
        ManagementService managementService = new ManagementServiceImpl(repository, auditLog);
        VerificationService verificationService = new VerificationServiceImpl(repository, auditLog);

        managementService.createIdentity(
                "ID-300",
                "Casey Harper",
                LocalDate.of(1994, 11, 5),
                OrganisationType.CENTRAL_AUTHORITY
        );

        VerificationResult result = verificationService.verify(
                new VerificationRequest("ID-300", OrganisationType.EMPLOYER, LocalDate.now())
        );

        assertTrue(result.exists());
        assertTrue(result.isValid());
    }

    @Test
    void rejectsRestrictedIdentityForDrivingLicenceAuthority() {
        InMemoryIdentityRepository repository = new InMemoryIdentityRepository();
        AuditLog auditLog = new AuditLog();
        ManagementService managementService = new ManagementServiceImpl(repository, auditLog);
        VerificationService verificationService = new VerificationServiceImpl(repository, auditLog);

        managementService.createIdentity(
                "ID-400",
                "Riley Singh",
                LocalDate.of(1988, 4, 22),
                OrganisationType.CENTRAL_AUTHORITY
        );
        managementService.setRestricted("ID-400", true, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest("ID-400", OrganisationType.DRIVING_LICENCE_AUTHORITY, LocalDate.now())
        );

        assertTrue(result.exists());
        assertFalse(result.isValid());
    }
}

