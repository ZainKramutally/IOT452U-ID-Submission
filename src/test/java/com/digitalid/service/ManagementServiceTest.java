package com.digitalid.service;

import com.digitalid.audit.AuditEvent;
import com.digitalid.audit.AuditLog;
import com.digitalid.domain.DigitalID;
import com.digitalid.domain.DigitalIDStatus;
import com.digitalid.domain.OrganisationType;
import com.digitalid.repository.InMemoryIdentityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ManagementServiceTest {

    private InMemoryIdentityRepository repository;
    private AuditLog auditLog;
    private ManagementService service;

    private static final String VALID_ID = "ID-001";
    private static final String VALID_NAME = "Taylor Quinn";
    private static final LocalDate VALID_DOB = LocalDate.of(1992, 3, 15);
    private static final String RESTRICTION_REASON = "LICENCE_REVIEW";
    private static final LocalDate RESTRICTION_EXPIRY = LocalDate.of(2099, 1, 1);

    @BeforeEach
    void setUp() {
        repository = new InMemoryIdentityRepository();
        auditLog = new AuditLog();
        service = new ManagementServiceImpl(repository, auditLog);
    }

    @Test
    void createIdentityReturnsDigitalIDWithCorrectFields() {
        DigitalID result = service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        assertEquals(VALID_ID, result.getId());
        assertEquals(VALID_NAME, result.getFullName());
        assertEquals(VALID_DOB, result.getDateOfBirth());
    }

    @Test
    void createIdentityDefaultsToActiveStatus() {
        DigitalID result = service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        assertEquals(DigitalIDStatus.ACTIVE, result.getStatus());
    }

    @Test
    void createIdentityPersistsToRepository() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        assertTrue(repository.exists(VALID_ID));
    }

    @Test
    void createIdentityRecordsAuditEvent() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        assertEquals(1, auditLog.getEvents().size());
        assertEquals("CREATE_IDENTITY", auditLog.getEvents().get(0).action());
    }

    @Test
    void createIdentityWithDuplicateIdThrowsIllegalStateException() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        assertThrows(IllegalStateException.class, () ->
                service.createIdentity(VALID_ID, "Another Name", VALID_DOB, OrganisationType.CENTRAL_AUTHORITY)
        );
    }

    @Test
    void createIdentityWithDuplicateIdStillRecordsAuditEvent() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        assertThrows(IllegalStateException.class, () ->
                service.createIdentity(VALID_ID, "Another Name", VALID_DOB, OrganisationType.CENTRAL_AUTHORITY)
        );

        assertTrue(auditLog.getEvents().size() >= 2);
    }

    @Test
    void createIdentityByNonCentralAuthorityThrowsSecurityException() {
        assertThrows(SecurityException.class, () ->
                service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.EMPLOYER)
        );
    }

    @Test
    void createIdentityByTaxAuthorityThrowsSecurityException() {
        assertThrows(SecurityException.class, () ->
                service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.TAX_AUTHORITY)
        );
    }

    @Test
    void createIdentityByDrivingLicenceAuthorityThrowsSecurityException() {
        assertThrows(SecurityException.class, () ->
                service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.DRIVING_LICENCE_AUTHORITY)
        );
    }

    @Test
    void createIdentityByBankThrowsSecurityException() {
        assertThrows(SecurityException.class, () ->
                service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.BANK)
        );
    }

    @Test
    void createIdentityByNonCentralAuthorityRecordsAuditEvent() {
        assertThrows(SecurityException.class, () ->
                service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.EMPLOYER)
        );

        assertFalse(auditLog.getEvents().isEmpty());
    }

    @Test
    void createIdentityWithBlankIdThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                service.createIdentity("   ", VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY)
        );
    }

    @Test
    void createIdentityWithNullIdThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                service.createIdentity(null, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY)
        );
    }

    @Test
    void createIdentityWithBlankNameThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                service.createIdentity(VALID_ID, "   ", VALID_DOB, OrganisationType.CENTRAL_AUTHORITY)
        );
    }

    @Test
    void createIdentityWithNullNameThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                service.createIdentity(VALID_ID, null, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY)
        );
    }

    @Test
    void createIdentityWithNullDateOfBirthThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                service.createIdentity(VALID_ID, VALID_NAME, null, OrganisationType.CENTRAL_AUTHORITY)
        );
    }

    @Test
    void createIdentityWithNullActorThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, null)
        );
    }

    @Test
    void createIdentityIdAndDateOfBirthAreImmutableAfterCreation() {
        DigitalID result = service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        service.updateName(VALID_ID, "New Name", OrganisationType.CENTRAL_AUTHORITY);

        assertEquals(VALID_ID, result.getId());
        assertEquals(VALID_DOB, result.getDateOfBirth());
    }

    @Test
    void updateNamePersistsNewName() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        service.updateName(VALID_ID, "New Name", OrganisationType.CENTRAL_AUTHORITY);

        assertEquals("New Name", repository.findById(VALID_ID).orElseThrow().getFullName());
    }

    @Test
    void updateNameRecordsAuditEvent() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        service.updateName(VALID_ID, "New Name", OrganisationType.CENTRAL_AUTHORITY);

        AuditEvent updateEvent = auditLog.getEvents().get(1);
        assertEquals("UPDATE_NAME", updateEvent.action());
    }

    @Test
    void updateNameOnNonExistentIdThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                service.updateName("NONEXISTENT", "New Name", OrganisationType.CENTRAL_AUTHORITY)
        );
    }

    @Test
    void updateNameByNonCentralAuthorityThrowsSecurityException() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        assertThrows(SecurityException.class, () ->
                service.updateName(VALID_ID, "New Name", OrganisationType.EMPLOYER)
        );
    }

    @Test
    void updateNameOnRevokedIdentityThrowsIllegalStateException() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        service.changeStatus(VALID_ID, DigitalIDStatus.REVOKED, OrganisationType.CENTRAL_AUTHORITY);

        assertThrows(IllegalStateException.class, () ->
                service.updateName(VALID_ID, "New Name", OrganisationType.CENTRAL_AUTHORITY)
        );
    }

    @Test
    void updateNameOnRevokedIdentityRecordsAuditEvent() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        service.changeStatus(VALID_ID, DigitalIDStatus.REVOKED, OrganisationType.CENTRAL_AUTHORITY);
        int eventsBefore = auditLog.getEvents().size();

        assertThrows(IllegalStateException.class, () ->
                service.updateName(VALID_ID, "New Name", OrganisationType.CENTRAL_AUTHORITY)
        );

        assertTrue(auditLog.getEvents().size() > eventsBefore);
    }

    @Test
    void updateNameOnSuspendedIdentitySucceeds() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        service.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);
        service.updateName(VALID_ID, "New Name", OrganisationType.CENTRAL_AUTHORITY);

        assertEquals("New Name", repository.findById(VALID_ID).orElseThrow().getFullName());
    }

    @Test
    void updateNameWithBlankNameThrowsIllegalArgumentException() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        assertThrows(IllegalArgumentException.class, () ->
                service.updateName(VALID_ID, "   ", OrganisationType.CENTRAL_AUTHORITY)
        );
    }

    @Test
    void updateNameWithNullNameThrowsNullPointerException() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        assertThrows(NullPointerException.class, () ->
                service.updateName(VALID_ID, null, OrganisationType.CENTRAL_AUTHORITY)
        );
    }

    @Test
    void changeStatusFromActiveToSuspendedSucceeds() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        service.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);

        assertEquals(DigitalIDStatus.SUSPENDED, repository.findById(VALID_ID).orElseThrow().getStatus());
    }

    @Test
    void changeStatusFromSuspendedToActiveSucceeds() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        service.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);
        service.changeStatus(VALID_ID, DigitalIDStatus.ACTIVE, OrganisationType.CENTRAL_AUTHORITY);

        assertEquals(DigitalIDStatus.ACTIVE, repository.findById(VALID_ID).orElseThrow().getStatus());
    }

    @Test
    void changeStatusFromActiveToRevokedSucceeds() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        service.changeStatus(VALID_ID, DigitalIDStatus.REVOKED, OrganisationType.CENTRAL_AUTHORITY);

        assertEquals(DigitalIDStatus.REVOKED, repository.findById(VALID_ID).orElseThrow().getStatus());
    }

    @Test
    void changeStatusFromSuspendedToRevokedSucceeds() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        service.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);
        service.changeStatus(VALID_ID, DigitalIDStatus.REVOKED, OrganisationType.CENTRAL_AUTHORITY);

        assertEquals(DigitalIDStatus.REVOKED, repository.findById(VALID_ID).orElseThrow().getStatus());
    }

    @Test
    void changeStatusAppendsToStatusHistory() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        service.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);

        DigitalID stored = repository.findById(VALID_ID).orElseThrow();
        assertEquals(2, stored.getStatusHistory().size());
    }

    @Test
    void changeStatusRecordsAuditEvent() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        service.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);

        AuditEvent statusEvent = auditLog.getEvents().get(1);
        assertEquals("CHANGE_STATUS", statusEvent.action());
    }

    @Test
    void changeStatusFromRevokedToActiveThrowsIllegalStateException() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        service.changeStatus(VALID_ID, DigitalIDStatus.REVOKED, OrganisationType.CENTRAL_AUTHORITY);

        assertThrows(IllegalStateException.class, () ->
                service.changeStatus(VALID_ID, DigitalIDStatus.ACTIVE, OrganisationType.CENTRAL_AUTHORITY)
        );
    }

    @Test
    void changeStatusFromRevokedToSuspendedThrowsIllegalStateException() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        service.changeStatus(VALID_ID, DigitalIDStatus.REVOKED, OrganisationType.CENTRAL_AUTHORITY);

        assertThrows(IllegalStateException.class, () ->
                service.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY)
        );
    }

    @Test
    void changeStatusRejectedTransitionRecordsAuditEvent() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        service.changeStatus(VALID_ID, DigitalIDStatus.REVOKED, OrganisationType.CENTRAL_AUTHORITY);
        int eventsBefore = auditLog.getEvents().size();

        assertThrows(IllegalStateException.class, () ->
                service.changeStatus(VALID_ID, DigitalIDStatus.ACTIVE, OrganisationType.CENTRAL_AUTHORITY)
        );

        assertTrue(auditLog.getEvents().size() > eventsBefore);
    }

    @Test
    void changeStatusNoOpReturnsSameIdentityWithoutAddingHistoryEntry() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        DigitalID before = repository.findById(VALID_ID).orElseThrow();
        int historySize = before.getStatusHistory().size();

        service.changeStatus(VALID_ID, DigitalIDStatus.ACTIVE, OrganisationType.CENTRAL_AUTHORITY);

        DigitalID after = repository.findById(VALID_ID).orElseThrow();
        assertEquals(historySize, after.getStatusHistory().size());
    }

    @Test
    void changeStatusNoOpRecordsAuditEvent() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        int eventsBefore = auditLog.getEvents().size();

        service.changeStatus(VALID_ID, DigitalIDStatus.ACTIVE, OrganisationType.CENTRAL_AUTHORITY);

        assertTrue(auditLog.getEvents().size() > eventsBefore);
    }

    @Test
    void changeStatusByNonCentralAuthorityThrowsSecurityException() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        assertThrows(SecurityException.class, () ->
                service.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.EMPLOYER)
        );
    }

    @Test
    void changeStatusOnNonExistentIdThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                service.changeStatus("NONEXISTENT", DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY)
        );
    }

    @Test
    void setRestrictedToTrueOnActiveIdentitySucceeds() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        service.setRestricted(VALID_ID, true, RESTRICTION_REASON, RESTRICTION_EXPIRY,
                OrganisationType.CENTRAL_AUTHORITY);

        assertTrue(repository.findById(VALID_ID).orElseThrow().isRestricted());
    }

    @Test
    void setRestrictedToFalseRemovesRestriction() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        service.setRestricted(VALID_ID, true, RESTRICTION_REASON, RESTRICTION_EXPIRY,
                OrganisationType.CENTRAL_AUTHORITY);
        service.setRestricted(VALID_ID, false, "LIFTED", null, OrganisationType.CENTRAL_AUTHORITY);

        assertFalse(repository.findById(VALID_ID).orElseThrow().isRestricted());
    }

    @Test
    void setRestrictedOnSuspendedIdentitySucceeds() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        service.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);
        service.setRestricted(VALID_ID, true, RESTRICTION_REASON, RESTRICTION_EXPIRY,
                OrganisationType.CENTRAL_AUTHORITY);

        assertTrue(repository.findById(VALID_ID).orElseThrow().isRestricted());
    }

    @Test
    void setRestrictedOnRevokedIdentityThrowsIllegalStateException() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        service.changeStatus(VALID_ID, DigitalIDStatus.REVOKED, OrganisationType.CENTRAL_AUTHORITY);

        assertThrows(IllegalStateException.class, () ->
                service.setRestricted(VALID_ID, true, RESTRICTION_REASON, RESTRICTION_EXPIRY,
                        OrganisationType.CENTRAL_AUTHORITY)
        );
    }

    @Test
    void setRestrictedOnRevokedIdentityRecordsAuditEvent() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        service.changeStatus(VALID_ID, DigitalIDStatus.REVOKED, OrganisationType.CENTRAL_AUTHORITY);
        int eventsBefore = auditLog.getEvents().size();

        assertThrows(IllegalStateException.class, () ->
                service.setRestricted(VALID_ID, true, RESTRICTION_REASON, RESTRICTION_EXPIRY,
                        OrganisationType.CENTRAL_AUTHORITY)
        );

        assertTrue(auditLog.getEvents().size() > eventsBefore);
    }

    @Test
    void setRestrictedByNonCentralAuthorityThrowsSecurityException() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);

        assertThrows(SecurityException.class, () ->
                service.setRestricted(VALID_ID, true, RESTRICTION_REASON, RESTRICTION_EXPIRY,
                        OrganisationType.EMPLOYER)
        );
    }

    @Test
    void setRestrictedOnNonExistentIdThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                service.setRestricted("NONEXISTENT", true, RESTRICTION_REASON, RESTRICTION_EXPIRY,
                        OrganisationType.CENTRAL_AUTHORITY)
        );
    }

    @Test
    void setRestrictedRecordsAuditEvent() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        service.setRestricted(VALID_ID, true, RESTRICTION_REASON, RESTRICTION_EXPIRY,
                OrganisationType.CENTRAL_AUTHORITY);

        AuditEvent event = auditLog.getEvents().get(1);
        assertEquals("SET_RESTRICTED", event.action());
        assertTrue(event.details().contains("reason=" + RESTRICTION_REASON));
        assertTrue(event.details().contains("expiresOn=" + RESTRICTION_EXPIRY));
    }

    @Test
    void changeStatusAuditRecordsCorrectedPreviousAndNewStatus() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        service.changeStatus(VALID_ID, DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);

        AuditEvent statusEvent = auditLog.getEvents().get(1);
        assertTrue(statusEvent.details().contains("from=ACTIVE"));
        assertTrue(statusEvent.details().contains("to=SUSPENDED"));
    }

    @Test
    void updateNameAuditRecordsOldAndNewName() {
        service.createIdentity(VALID_ID, VALID_NAME, VALID_DOB, OrganisationType.CENTRAL_AUTHORITY);
        service.updateName(VALID_ID, "New Name", OrganisationType.CENTRAL_AUTHORITY);

        AuditEvent updateEvent = auditLog.getEvents().get(1);
        assertTrue(updateEvent.details().contains("from=" + VALID_NAME));
        assertTrue(updateEvent.details().contains("to=New Name"));
    }
}

