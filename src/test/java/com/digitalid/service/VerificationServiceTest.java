package com.digitalid.service;

import com.digitalid.audit.AuditActions;
import com.digitalid.audit.AuditEvent;
import com.digitalid.audit.AuditLog;
import com.digitalid.audit.AuditReasons;
import com.digitalid.domain.OrganisationType;
import com.digitalid.domain.ReasonCode;
import com.digitalid.domain.DigitalIDStatus;
import com.digitalid.repository.InMemoryIdentityRepository;
import com.digitalid.verification.VerificationRequest;
import com.digitalid.verification.VerificationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class VerificationServiceTest {

    private AuditLog auditLog;
    private ManagementService managementService;
    private VerificationService verificationService;

    private static final LocalDate TODAY_UTC = LocalDate.now(ZoneOffset.UTC);
    private static final String VALID_ID = "ID-001";
    private static final String VALID_NAME = "Casey Harper";
    private static final LocalDate VALID_DOB = LocalDate.of(1994, 11, 5);
    private static final String RESTRICTION_REASON = "LICENCE_REVIEW";
    private static final LocalDate RESTRICTION_EXPIRY = LocalDate.of(2099, 1, 1);

    @BeforeEach
    void setUp() {
        InMemoryIdentityRepository repository = new InMemoryIdentityRepository();
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

        assertFalse(result.valid());
    }

    @Test
    void verifyingNonExistentIdReturnsNotFoundReasonCode() {
        VerificationResult result = verificationService.verify(
                new VerificationRequest("NONEXISTENT", OrganisationType.EMPLOYER, null, null)
        );

        assertEquals(ReasonCode.NOT_FOUND, result.reason());
    }

    @Test
    void verifyingNonExistentIdAsEmployerReturnsNotFound() {
        VerificationResult result = verificationService.verify(
                new VerificationRequest("NONEXISTENT", OrganisationType.EMPLOYER, null, null)
        );

        assertFalse(result.exists());
        assertEquals(ReasonCode.NOT_FOUND, result.reason());
    }

    @Test
    void verifyingNonExistentIdAsBankReturnsNotFound() {
        VerificationResult result = verificationService.verify(
                new VerificationRequest("NONEXISTENT", OrganisationType.BANK, null, null)
        );

        assertFalse(result.exists());
        assertEquals(ReasonCode.NOT_FOUND, result.reason());
    }

    @Test
    void verifyingNonExistentIdAsDrivingLicenceAuthorityReturnsNotFound() {
        VerificationResult result = verificationService.verify(
                new VerificationRequest("NONEXISTENT", OrganisationType.DRIVING_LICENCE_AUTHORITY, null, null)
        );

        assertFalse(result.exists());
        assertEquals(ReasonCode.NOT_FOUND, result.reason());
    }

    @Test
    void verifyingNonExistentIdAsTaxAuthorityReturnsNotFound() {
        VerificationResult result = verificationService.verify(
                new VerificationRequest("NONEXISTENT", OrganisationType.TAX_AUTHORITY,
                        TODAY_UTC.minusDays(30), TODAY_UTC.plusDays(30))
        );

        assertFalse(result.exists());
        assertEquals(ReasonCode.NOT_FOUND, result.reason());
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

        assertTrue(result.valid());
    }

    @Test
    void activeIdentityVerifiedByEmployerReturnsNoDetail() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.EMPLOYER, null, null)
        );

        assertNull(result.detail());
    }

    @Test
    void suspendedIdentityVerifiedByEmployerReturnsValidFalse() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.EMPLOYER, null, null)
        );

        assertFalse(result.valid());
    }

    @Test
    void suspendedIdentityVerifiedByEmployerReturnsNoDetail() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.EMPLOYER, null, null)
        );

        assertNull(result.detail());
    }

    @Test
    void revokedIdentityVerifiedByEmployerReturnsValidFalse() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.REVOKED, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.EMPLOYER, null, null)
        );

        assertFalse(result.valid());
    }

    @Test
    void restrictedIdentityVerifiedByEmployerReturnsValidTrue() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.setRestricted(VALID_ID, true, RESTRICTION_REASON, RESTRICTION_EXPIRY,
                OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.EMPLOYER, null, null)
        );

        assertTrue(result.valid());
    }

    @Test
    void activeIdentityVerifiedByBankReturnsValidTrue() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.BANK, null, null)
        );

        assertTrue(result.valid());
    }

    @Test
    void activeIdentityVerifiedByBankReturnsNoDetail() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.BANK, null, null)
        );

        assertNull(result.detail());
    }

    @Test
    void suspendedIdentityVerifiedByBankReturnsValidFalse() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.BANK, null, null)
        );

        assertFalse(result.valid());
    }

    @Test
    void suspendedIdentityVerifiedByBankReturnsNoDetail() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.BANK, null, null)
        );

        assertNull(result.detail());
    }

    @Test
    void restrictedIdentityVerifiedByBankReturnsValidTrue() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.setRestricted(VALID_ID, true, RESTRICTION_REASON, RESTRICTION_EXPIRY,
                OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.BANK, null, null)
        );

        assertTrue(result.valid());
    }

    @Test
    void activeUnrestrictedIdentityVerifiedByDrivingLicenceAuthorityReturnsValidTrue() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.DRIVING_LICENCE_AUTHORITY, null, null)
        );

        assertTrue(result.valid());
    }

    @Test
    void activeRestrictedIdentityVerifiedByDrivingLicenceAuthorityReturnsValidFalse() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.setRestricted(VALID_ID, true, RESTRICTION_REASON, RESTRICTION_EXPIRY,
                OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.DRIVING_LICENCE_AUTHORITY, null, null)
        );

        assertFalse(result.valid());
    }

    @Test
    void activeRestrictedIdentityVerifiedByDrivingLicenceAuthorityReturnsRestrictedReason() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.setRestricted(VALID_ID, true, RESTRICTION_REASON, RESTRICTION_EXPIRY,
                OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.DRIVING_LICENCE_AUTHORITY, null, null)
        );

        assertEquals(ReasonCode.RESTRICTED, result.reason());
    }

    @Test
    void suspendedIdentityVerifiedByDrivingLicenceAuthorityReturnsValidFalse() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.DRIVING_LICENCE_AUTHORITY, null, null)
        );

        assertFalse(result.valid());
    }

    @Test
    void suspendedIdentityVerifiedByDrivingLicenceAuthorityReturnsInactiveReason() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.DRIVING_LICENCE_AUTHORITY, null, null)
        );

        assertEquals(ReasonCode.INACTIVE, result.reason());
    }

    @Test
    void revokedIdentityVerifiedByDrivingLicenceAuthorityReturnsValidFalse() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.REVOKED, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.DRIVING_LICENCE_AUTHORITY, null, null)
        );

        assertFalse(result.valid());
    }

    @Test
    void activeIdentityWithNoSuspensionsInPeriodVerifiedByTaxAuthorityReturnsValidTrue() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.TAX_AUTHORITY,
                        TODAY_UTC.minusDays(30), TODAY_UTC.plusDays(30))
        );

        assertTrue(result.valid());
    }

    @Test
    void activeIdentityWithNoSuspensionsInPeriodVerifiedByTaxAuthorityReturnsValidReason() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.TAX_AUTHORITY,
                        TODAY_UTC.minusDays(30), TODAY_UTC.plusDays(30))
        );

        assertEquals(ReasonCode.VALID, result.reason());
    }

    @Test
    void identityCurrentlySuspendedVerifiedByTaxAuthorityReturnsValidFalse() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.TAX_AUTHORITY,
                        TODAY_UTC.minusDays(30), TODAY_UTC.plusDays(30))
        );

        assertFalse(result.valid());
    }

    @Test
    void identityCurrentlySuspendedVerifiedByTaxAuthorityReturnsSuspendedDuringPeriodReason() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.TAX_AUTHORITY,
                        TODAY_UTC.minusDays(30), TODAY_UTC.plusDays(30))
        );

        assertEquals(ReasonCode.SUSPENDED_DURING_PERIOD, result.reason());
    }

    @Test
    void revokedIdentityVerifiedByTaxAuthorityReturnsValidFalse() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        managementService.changeStatus(VALID_ID, DigitalIDStatus.REVOKED, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.TAX_AUTHORITY,
                        TODAY_UTC.minusDays(30), TODAY_UTC.plusDays(30))
        );

        assertFalse(result.valid());
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
    void centralAuthorityCallingVerifyRecordsRejectedAuditEvent() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        int eventsBefore = auditLog.getEvents().size();

        assertThrows(SecurityException.class, () ->
                verificationService.verify(
                        new VerificationRequest(VALID_ID, OrganisationType.CENTRAL_AUTHORITY, null, null)
                )
        );

        assertTrue(auditLog.getEvents().size() > eventsBefore);
        AuditEvent event = auditLog.getEvents().get(auditLog.getEvents().size() - 1);
        assertEquals(AuditActions.rejected(AuditActions.VERIFY), event.action());
        assertTrue(event.details().contains("id=" + VALID_ID));
        assertTrue(event.details().contains("org=" + OrganisationType.CENTRAL_AUTHORITY));
        assertTrue(event.details().contains("reason=UNAUTHORISED"));
    }

    @Test
    void centralAuthorityCallingVerifyOnMissingIdThrowsSecurityException() {
        assertThrows(SecurityException.class, () ->
                verificationService.verify(
                        new VerificationRequest("NONEXISTENT", OrganisationType.CENTRAL_AUTHORITY, null, null)
                )
        );
    }

    @Test
    void centralAuthorityCallingVerifyOnMissingIdRecordsRejectedAuditEvent() {
        int eventsBefore = auditLog.getEvents().size();

        assertThrows(SecurityException.class, () ->
                verificationService.verify(
                        new VerificationRequest("NONEXISTENT", OrganisationType.CENTRAL_AUTHORITY, null, null)
                )
        );

        assertTrue(auditLog.getEvents().size() > eventsBefore);
        AuditEvent event = auditLog.getEvents().get(auditLog.getEvents().size() - 1);
        assertEquals(AuditActions.rejected(AuditActions.VERIFY), event.action());
        assertTrue(event.details().contains("id=NONEXISTENT"));
        assertTrue(event.details().contains("org=" + OrganisationType.CENTRAL_AUTHORITY));
        assertTrue(event.details().contains("reason=UNAUTHORISED"));
    }

    @Test
    void taxAuthorityRequestWithNullPeriodStartThrowsIllegalArgumentException() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        assertThrows(IllegalArgumentException.class, () ->
                verificationService.verify(
                        new VerificationRequest(VALID_ID, OrganisationType.TAX_AUTHORITY,
                                null, TODAY_UTC.plusDays(30))
                )
        );
    }

    @Test
    void taxAuthorityRequestWithNullPeriodStartRecordsRejectedAuditEvent() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        int eventsBefore = auditLog.getEvents().size();

        assertThrows(IllegalArgumentException.class, () ->
                verificationService.verify(
                        new VerificationRequest(VALID_ID, OrganisationType.TAX_AUTHORITY,
                                null, TODAY_UTC.plusDays(30))
                )
        );

        AuditEvent event = auditLog.getEvents().get(auditLog.getEvents().size() - 1);
        assertTrue(auditLog.getEvents().size() > eventsBefore);
        assertEquals(AuditActions.rejected(AuditActions.VERIFY), event.action());
        assertTrue(event.details().contains("id=" + VALID_ID));
        assertTrue(event.details().contains("org=" + OrganisationType.TAX_AUTHORITY));
        assertTrue(event.details().contains("reason=MISSING_PERIOD_START"));
    }

    @Test
    void taxAuthorityRequestWithNullPeriodEndThrowsIllegalArgumentException() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        assertThrows(IllegalArgumentException.class, () ->
                verificationService.verify(
                        new VerificationRequest(VALID_ID, OrganisationType.TAX_AUTHORITY,
                                TODAY_UTC.minusDays(30), null)
                )
        );
    }

    @Test
    void taxAuthorityRequestWithNullPeriodEndRecordsRejectedAuditEvent() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        int eventsBefore = auditLog.getEvents().size();

        assertThrows(IllegalArgumentException.class, () ->
                verificationService.verify(
                        new VerificationRequest(VALID_ID, OrganisationType.TAX_AUTHORITY,
                                TODAY_UTC.minusDays(30), null)
                )
        );

        AuditEvent event = auditLog.getEvents().get(auditLog.getEvents().size() - 1);
        assertTrue(auditLog.getEvents().size() > eventsBefore);
        assertEquals(AuditActions.rejected(AuditActions.VERIFY), event.action());
        assertTrue(event.details().contains("id=" + VALID_ID));
        assertTrue(event.details().contains("org=" + OrganisationType.TAX_AUTHORITY));
        assertTrue(event.details().contains("reason=MISSING_PERIOD_END"));
    }

    @Test
    void verificationResultAlwaysHasNonNullReason() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult employer = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.EMPLOYER, null, null)
        );
        VerificationResult bank = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.BANK, null, null)
        );
        VerificationResult driving = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.DRIVING_LICENCE_AUTHORITY, null, null)
        );
        VerificationResult tax = verificationService.verify(
                new VerificationRequest(VALID_ID, OrganisationType.TAX_AUTHORITY,
                        TODAY_UTC.minusDays(30), TODAY_UTC.plusDays(30))
        );
        VerificationResult notFound = verificationService.verify(
                new VerificationRequest("NONEXISTENT", OrganisationType.EMPLOYER, null, null)
        );

        assertNotNull(employer.reason());
        assertNotNull(bank.reason());
        assertNotNull(driving.reason());
        assertNotNull(tax.reason());
        assertNotNull(notFound.reason());
    }

    @Test
    void nullRequestRecordsRejectedAuditEvent() {
        int eventsBefore = auditLog.getEvents().size();

        assertThrows(NullPointerException.class, () ->
                verificationService.verify(null)
        );

        AuditEvent event = auditLog.getEvents().get(auditLog.getEvents().size() - 1);
        assertTrue(auditLog.getEvents().size() > eventsBefore);
        assertEquals(AuditActions.rejected(AuditActions.VERIFY), event.action());
        assertTrue(event.details().contains("reason=" + AuditReasons.MISSING_REQUEST));
    }

    @Test
    void blankIdRequestRecordsRejectedAuditEvent() {
        int eventsBefore = auditLog.getEvents().size();

        assertThrows(IllegalArgumentException.class, () ->
                verificationService.verify(
                        new VerificationRequest("   ", OrganisationType.EMPLOYER, null, null)
                )
        );

        AuditEvent event = auditLog.getEvents().get(auditLog.getEvents().size() - 1);
        assertTrue(auditLog.getEvents().size() > eventsBefore);
        assertEquals(AuditActions.rejected(AuditActions.VERIFY), event.action());
        assertTrue(event.details().contains("reason=" + AuditReasons.MISSING_ID));
    }

    @Test
    void verifyWithNullOrganisationTypeRecordsAuditEvent() {
        int eventsBefore = auditLog.getEvents().size();

        assertThrows(NullPointerException.class, () ->
                verificationService.verify(new VerificationRequest(VALID_ID, null, null, null))
        );

        AuditEvent event = auditLog.getEvents().get(auditLog.getEvents().size() - 1);
        assertTrue(auditLog.getEvents().size() > eventsBefore);
        assertEquals(AuditActions.rejected(AuditActions.VERIFY), event.action());
        assertTrue(event.details().contains("reason=" + AuditReasons.MISSING_ORG));
    }

    @Test
    void verifyWithNullDigitalIdRecordsAuditEvent() {
        int eventsBefore = auditLog.getEvents().size();

        assertThrows(NullPointerException.class, () ->
                verificationService.verify(new VerificationRequest(null, OrganisationType.EMPLOYER, null, null))
        );

        AuditEvent event = auditLog.getEvents().get(auditLog.getEvents().size() - 1);
        assertTrue(auditLog.getEvents().size() > eventsBefore);
        assertEquals(AuditActions.rejected(AuditActions.VERIFY), event.action());
        assertTrue(event.details().contains("reason=" + AuditReasons.MISSING_ID));
    }

    @Test
    void taxAuthorityWithInvalidPeriodRangeRecordsAuditEvent() {
        managementService.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        int eventsBefore = auditLog.getEvents().size();

        assertThrows(IllegalArgumentException.class, () ->
                verificationService.verify(new VerificationRequest(VALID_ID, OrganisationType.TAX_AUTHORITY,
                        TODAY_UTC.plusDays(1), TODAY_UTC.minusDays(1)))
        );

        AuditEvent event = auditLog.getEvents().get(auditLog.getEvents().size() - 1);
        assertTrue(auditLog.getEvents().size() > eventsBefore);
        assertEquals(AuditActions.rejected(AuditActions.VERIFY), event.action());
        assertTrue(event.details().contains("reason=" + AuditReasons.INVALID_PERIOD_RANGE));
    }

    @Test
    void centralAuthorityWithNullIdStillThrowsSecurityException() {
        assertThrows(SecurityException.class, () ->
                verificationService.verify(
                        new VerificationRequest(null, OrganisationType.CENTRAL_AUTHORITY, null, null)
                )
        );
    }

    @Test
    void centralAuthorityWithNullIdRecordsUnauthorisedRejection() {
        int eventsBefore = auditLog.getEvents().size();

        assertThrows(SecurityException.class, () ->
                verificationService.verify(
                        new VerificationRequest(null, OrganisationType.CENTRAL_AUTHORITY, null, null)
                )
        );

        AuditEvent event = auditLog.getEvents().get(auditLog.getEvents().size() - 1);
        assertTrue(auditLog.getEvents().size() > eventsBefore);
        assertEquals(AuditActions.rejected(AuditActions.VERIFY), event.action());
        assertTrue(event.details().contains("org=" + OrganisationType.CENTRAL_AUTHORITY));
        assertTrue(event.details().contains("reason=" + AuditReasons.UNAUTHORISED));
    }

    @Test
    void centralAuthorityWithBlankIdStillThrowsSecurityException() {
        assertThrows(SecurityException.class, () ->
                verificationService.verify(
                        new VerificationRequest("   ", OrganisationType.CENTRAL_AUTHORITY, null, null)
                )
        );
    }

    @Test
    void centralAuthorityWithBlankIdRecordsUnauthorisedRejection() {
        int eventsBefore = auditLog.getEvents().size();

        assertThrows(SecurityException.class, () ->
                verificationService.verify(
                        new VerificationRequest("   ", OrganisationType.CENTRAL_AUTHORITY, null, null)
                )
        );

        AuditEvent event = auditLog.getEvents().get(auditLog.getEvents().size() - 1);
        assertTrue(auditLog.getEvents().size() > eventsBefore);
        assertEquals(AuditActions.rejected(AuditActions.VERIFY), event.action());
        assertTrue(event.details().contains("org=" + OrganisationType.CENTRAL_AUTHORITY));
        assertTrue(event.details().contains("reason=" + AuditReasons.UNAUTHORISED));
    }
}
