package com.digitalid.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

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
        digitalID.setRestricted(true);

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
    void statusHistoryIsUnmodifiable() {
        DigitalID digitalID = new DigitalID("ID-5", "Jordan Blake", LocalDate.of(1988, 7, 15));

        assertThrows(UnsupportedOperationException.class, () ->
                digitalID.getStatusHistory().clear()
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

