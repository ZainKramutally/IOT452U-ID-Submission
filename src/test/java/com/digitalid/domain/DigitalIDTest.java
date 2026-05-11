package com.digitalid.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DigitalIDTest {

    @Test
    void updatesNameAndStatus() {
        DigitalID digitalID = new DigitalID("ID-1", "Sam Reed", LocalDate.of(1990, 1, 1));

        digitalID.updateFullName("Samira Reed");
        digitalID.recordStatusChange(DigitalIDStatus.SUSPENDED);

        assertEquals("Samira Reed", digitalID.getFullName());
        assertEquals(DigitalIDStatus.SUSPENDED, digitalID.getStatus());
    }

    @Test
    void keepsImmutableFields() {
        LocalDate dob = LocalDate.of(1985, 6, 20);
        DigitalID digitalID = new DigitalID("ID-2", "Jordan Lee", dob);

        digitalID.updateFullName("Jordan L.");
        digitalID.setRestricted(true, "REVIEW", LocalDate.of(2099, 1, 1));

        assertEquals("ID-2", digitalID.getId());
        assertEquals(dob, digitalID.getDateOfBirth());
        assertTrue(digitalID.isRestricted());
    }

    @Test
    void newIdentityDefaultsToActiveWithOneHistoryEntry() {
        DigitalID digitalID = new DigitalID("ID-3", "Alex Smith", LocalDate.of(1990, 5, 1));

        assertEquals(DigitalIDStatus.ACTIVE, digitalID.getStatus());
        assertEquals(1, digitalID.getStatusHistory().size());
        assertEquals(DigitalIDStatus.ACTIVE, digitalID.getStatusHistory().get(0).status());
    }

    @Test
    void recordStatusChangeAppendsToHistory() {
        DigitalID digitalID = new DigitalID("ID-4", "Sam Jones", LocalDate.of(1992, 3, 10));

        digitalID.recordStatusChange(DigitalIDStatus.SUSPENDED);
        digitalID.recordStatusChange(DigitalIDStatus.ACTIVE);

        assertEquals(3, digitalID.getStatusHistory().size());
        assertEquals(DigitalIDStatus.ACTIVE, digitalID.getStatus());
    }

    @Test
    void restrictionHistoryAppendsWithDetails() {
        DigitalID digitalID = new DigitalID("ID-7", "Rory Lane", LocalDate.of(1991, 2, 10));

        digitalID.setRestricted(true, "LICENCE_REVIEW", LocalDate.of(2099, 1, 1));
        digitalID.setRestricted(false, "LIFTED", null);

        assertEquals(2, digitalID.getRestrictionHistory().size());
        RestrictionChange latest = digitalID.getRestrictionHistory().get(1);
        assertFalse(latest.restricted());
        assertEquals("LIFTED", latest.reason());
    }

    @Test
    void restrictionExpiresWhenPastDate() {
        DigitalID digitalID = new DigitalID("ID-8", "Sam Park", LocalDate.of(1989, 4, 12));

        digitalID.setRestricted(true, "TEMP_HOLD", LocalDate.now(ZoneOffset.UTC).minusDays(1));

        assertFalse(digitalID.isRestricted());
    }

    @Test
    void restrictionHistoryIsUnmodifiable() {
        DigitalID digitalID = new DigitalID("ID-9", "Dee Chen", LocalDate.of(1995, 9, 3));

        assertThrows(UnsupportedOperationException.class, () ->
                digitalID.getRestrictionHistory().clear()
        );
    }

    @Test
    void blankIdThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                new DigitalID("  ", "Test Name", LocalDate.of(1990, 1, 1))
        );
    }

    @Test
    void nullDateOfBirthThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                new DigitalID("ID-6", "Test Name", null)
        );
    }

    @Test
    void canTransitionToReflectsValidTransitions() {
        assertTrue(DigitalIDStatus.ACTIVE.canTransitionTo(DigitalIDStatus.SUSPENDED));
        assertTrue(DigitalIDStatus.ACTIVE.canTransitionTo(DigitalIDStatus.REVOKED));
        assertTrue(DigitalIDStatus.SUSPENDED.canTransitionTo(DigitalIDStatus.ACTIVE));
        assertTrue(DigitalIDStatus.SUSPENDED.canTransitionTo(DigitalIDStatus.REVOKED));
        assertFalse(DigitalIDStatus.REVOKED.canTransitionTo(DigitalIDStatus.ACTIVE));
        assertFalse(DigitalIDStatus.REVOKED.canTransitionTo(DigitalIDStatus.SUSPENDED));
    }
}
