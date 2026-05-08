package com.digitalid.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}

