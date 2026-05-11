package com.digitalid.service;

import com.digitalid.audit.AuditLog;
import com.digitalid.domain.OrganisationType;
import com.digitalid.domain.ReasonCode;
import com.digitalid.domain.DigitalIDStatus;
import com.digitalid.repository.InMemoryIdentityRepository;
import com.digitalid.verification.VerificationRequest;
import com.digitalid.verification.VerificationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class VerificationServiceTest {

    private InMemoryIdentityRepository repository;
    private AuditLog auditLog;
    private ManagementService managementService;
    private VerificationService verificationService;

    private static final String VALID_ID = "ID-001";
    private static final String VALID_NAME = "Casey Harper";
    private static final LocalDate VALID_DOB = LocalDate.of(1994, 11, 5);

    @BeforeEach
    void setUp() {
        repository = new InMemoryIdentityRepository();
        auditLog = new AuditLog();
        managementService = new ManagementServiceImpl(repository, auditLog);
        verificationService = new VerificationServiceImpl(repository, auditLog);
    }

    @Test
    void verifyingNonExistentIdReturnsExistsFalse() {
        VerificationResult result = verificationService.verify(
                new VerificationRequest("NONEXISTENT", OrganisationType.EMPLOYER, null, null)
        );

        assertFalse(result.exists());
    }

    @Test
    void verifyingNonExistentIdReturnsValidFalse() {
        VerificationResult result = verificationService.verify(
                new VerificationRequest("NONEXISTENT", OrganisationType.EMPLOYER, null, null)
        );

        assertFalse(result.isValid());
    }

    @Test
    void verifyingNonExistentIdReturnsNotFoundReasonCode() {
        VerificationResult result = verificationService.verify(
                new VerificationRequest("NONEXISTENT", OrganisationType.EMPLOYER, null, null)
        );

        assertEquals(ReasonCode.NOT_FOUND, result.getReason());
    }

    @Test
    void verifyingNonExistentIdAsEmployerReturnsNotFound() {
        VerificationResult result = verificationService.verify(
                new VerificationRequest("NONEXISTENT", OrganisationType.EMPLOYER, null, null)
        );

        assertFalse(result.exists());
        assertEquals(ReasonCode.NOT_FOUND, result.getReason());
    }

    @Test
    void verifyingNonExistentIdAsBankReturnsNotFound() {
        VerificationResult result = verificationService.verify(
                new VerificationRequest("NONEXISTENT", OrganisationType.BANK, null, null)
        );

        assertFalse(result.exists());
        assertEquals(ReasonCode.NOT_FOUND, result.getReason());
    }

    @Test
    void verifyingNonExistentIdAsDrivingLicenceAuthorityReturnsNotFound() {
        VerificationResult result = verificationService.verify(
                new VerificationRequest("NONEXISTENT", OrganisationType.DRIVING_LICENCE_AUTHORITY, null, null)
        );

        assertFalse(result.exists());
        assertEquals(ReasonCode.NOT_FOUND, result.getReason());
    }

    @Test
    void verifyingNonExistentIdAsTaxAuthorityReturnsNotFound() {
        VerificationResult result = verificationService.verify(
                new VerificationRequest("NONEXISTENT", OrganisationType.TAX_AUTHORITY,
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        );

        assertFalse(result.exists());
        assertEquals(ReasonCode.NOT_FOUND, result.getReason());
    }

    @Test
    void verifyingNonExistentIdRecordsAuditEvent() {
        verificationService.verify(
                new VerificationRequest("NONEXISTENT", OrganisationType.EMPLOYER, null, null)
        );

        assertFalse(auditLog.getEvents().isEmpty());
    }

    @Test
    void activeIdentityVerifiedByEmployerReturnsExistsTrue() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.EMPLOYER, null, null)
        );

        assertTrue(result.exists());
    }

    @Test
    void activeIdentityVerifiedByEmployerReturnsValidTrue() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.EMPLOYER, null, null)
        );

        assertTrue(result.isValid());
    }

    @Test
    void activeIdentityVerifiedByEmployerReturnsNoDetail() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.EMPLOYER, null, null)
        );

        assertNull(result.getDetail());
    }

    @Test
    void suspendedIdentityVerifiedByEmployerReturnsValidFalse() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.EMPLOYER, null, null)
        );

        assertFalse(result.isValid());
    }

    @Test
    void suspendedIdentityVerifiedByEmployerReturnsNoDetail() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.EMPLOYER, null, null)
        );

        assertNull(result.getDetail());
    }

    @Test
    void revokedIdentityVerifiedByEmployerReturnsValidFalse() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.REVOKED, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.EMPLOYER, null, null)
        );

        assertFalse(result.isValid());
    }

    @Test
    void restrictedIdentityVerifiedByEmployerReturnsValidTrue() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.setRestricted(VALID_ID, true, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.EMPLOYER, null, null)
        );

        assertTrue(result.isValid());
    }

    @Test
    void activeIdentityVerifiedByBankReturnsValidTrue() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.BANK, null, null)
        );

        assertTrue(result.isValid());
    }

    @Test
    void activeIdentityVerifiedByBankReturnsNoDetail() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.BANK, null, null)
        );

        assertNull(result.getDetail());
    }

    @Test
    void suspendedIdentityVerifiedByBankReturnsValidFalse() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.BANK, null, null)
        );

        assertFalse(result.isValid());
    }

    @Test
    void suspendedIdentityVerifiedByBankReturnsNoDetail() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.BANK, null, null)
        );

        assertNull(result.getDetail());
    }

    @Test
    void restrictedIdentityVerifiedByBankReturnsValidTrue() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.setRestricted(VALID_ID, true, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.BANK, null, null)
        );

        assertTrue(result.isValid());
    }

    @Test
    void activeUnrestrictedIdentityVerifiedByDrivingLicenceAuthorityReturnsValidTrue() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.DRIVING_LICENCE_AUTHORITY, null, null)
        );

        assertTrue(result.isValid());
    }

    @Test
    void activeRestrictedIdentityVerifiedByDrivingLicenceAuthorityReturnsValidFalse() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.setRestricted(VALID_ID, true, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.DRIVING_LICENCE_AUTHORITY, null, null)
        );

        assertFalse(result.isValid());
    }

    @Test
    void activeRestrictedIdentityVerifiedByDrivingLicenceAuthorityReturnsRestrictedReason() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.setRestricted(VALID_ID, true, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.DRIVING_LICENCE_AUTHORITY, null, null)
        );

        assertEquals(ReasonCode.RESTRICTED, result.getReason());
    }

    @Test
    void suspendedIdentityVerifiedByDrivingLicenceAuthorityReturnsValidFalse() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.DRIVING_LICENCE_AUTHORITY, null, null)
        );

        assertFalse(result.isValid());
    }

    @Test
    void suspendedIdentityVerifiedByDrivingLicenceAuthorityReturnsInactiveReason() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.DRIVING_LICENCE_AUTHORITY, null, null)
        );

        assertEquals(ReasonCode.INACTIVE, result.getReason());
    }

    @Test
    void revokedIdentityVerifiedByDrivingLicenceAuthorityReturnsValidFalse() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.REVOKED, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.DRIVING_LICENCE_AUTHORITY, null, null)
        );

        assertFalse(result.isValid());
    }

    @Test
    void activeIdentityWithNoSuspensionsInPeriodVerifiedByTaxAuthorityReturnsValidTrue() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.TAX_AUTHORITY,
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        );

        assertTrue(result.isValid());
    }

    @Test
    void activeIdentityWithNoSuspensionsInPeriodVerifiedByTaxAuthorityReturnsValidReason() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.TAX_AUTHORITY,
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        );

        assertEquals(ReasonCode.VALID, result.getReason());
    }

    @Test
    void identityCurrentlySuspendedVerifiedByTaxAuthorityReturnsValidFalse() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.TAX_AUTHORITY,
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        );

        assertFalse(result.isValid());
    }

    @Test
    void identityCurrentlySuspendedVerifiedByTaxAuthorityReturnsSuspendedDuringPeriodReason() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.TAX_AUTHORITY,
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        );

        assertEquals(ReasonCode.SUSPENDED_DURING_PERIOD, result.getReason());
    }

    @Test
    void identitySuspendedThenReinstatedBeforePeriodVerifiedByTaxAuthorityReturnsValidTrue() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.ACTIVE, OrganisationType.CENTRAL_AUTHORITY);

        // period is in the future so no suspension falls within it
        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.TAX_AUTHORITY,
                        LocalDate.now().plusYears(1), LocalDate.now().plusYears(2))
        );

        assertTrue(result.isValid());
    }

    @Test
    void identitySuspendedDuringPeriodThenReinstatedVerifiedByTaxAuthorityReturnsValidFalse() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.ACTIVE, OrganisationType.CENTRAL_AUTHORITY);

        // period covers now so the suspension that happened now falls within it
        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.TAX_AUTHORITY,
                        LocalDate.now().minusDays(1), LocalDate.now().plusDays(1))
        );

        assertFalse(result.isValid());
    }

    @Test
    void identitySuspendedDuringPeriodThenReinstatedReturnsSuspendedDuringPeriodReason() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.ACTIVE, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.TAX_AUTHORITY,
                        LocalDate.now().minusDays(1), LocalDate.now().plusDays(1))
        );

        assertEquals(ReasonCode.SUSPENDED_DURING_PERIOD, result.getReason());
    }

    @Test
    void revokedIdentityVerifiedByTaxAuthorityReturnsValidFalse() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.REVOKED, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.TAX_AUTHORITY,
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        );

        assertFalse(result.isValid());
    }

    @Test
    void everyVerificationRequestRecordsAnAuditEvent() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        int eventsBefore = auditLog.getEvents().size();

        verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.EMPLOYER, null, null)
        );

        assertTrue(auditLog.getEvents().size() > eventsBefore);
    }

    @Test
    void notFoundVerificationRecordsAuditEvent() {
        int eventsBefore = auditLog.getEvents().size();

        verificationService.verify(
                new VerificationRequest("NONEXISTENT", OrganisationType.EMPLOYER, null, null)
        );

        assertTrue(auditLog.getEvents().size() > eventsBefore);
    }

    @Test
    void nullRequestThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                verificationService.verify(null)
        );
    }

    @Test
    void centralAuthorityCallingVerifyThrowsSecurityException() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        assertThrows(SecurityException.class, () ->
                verificationService.verify(
                        new VerificationRequest(VALID_ID, OrganisationType.CENTRAL_AUTHORITY, null, null)
                )
        );
    }

    @Test
    void taxAuthorityRequestWithNullPeriodStartThrowsIllegalArgumentException() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        assertThrows(IllegalArgumentException.class, () ->
                verificationService.verify(
                        new VerificationRequest(VALID_ID, OrganisationType.TAX_AUTHORITY, null, LocalDate.of(2026, 12, 31))
                )
        );
    }

    @Test
    void taxAuthorityRequestWithNullPeriodEndThrowsIllegalArgumentException() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        assertThrows(IllegalArgumentException.class, () ->
                verificationService.verify(
                        new VerificationRequest(VALID_ID, OrganisationType.TAX_AUTHORITY, LocalDate.of(2026, 1, 1), null)
                )
        );
    }

    @Test
    void employerResultReasonIsNotExposedAsDetail() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.EMPLOYER, null, null)
        );

        assertNull(result.getDetail());
        assertNotNull(result.getReason());
    }

    @Test
    void bankResultReasonIsNotExposedAsDetail() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.BANK, null, null)
        );

        assertNull(result.getDetail());
        assertNotNull(result.getReason());
    }
}