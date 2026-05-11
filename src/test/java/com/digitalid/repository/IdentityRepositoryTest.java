package com.digitalid.repository;

import com.digitalid.domain.DigitalID;
import com.digitalid.domain.DigitalIDStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class IdentityRepositoryTest {

    private IdentityRepository repository;

    private static final LocalDate VALID_DOB = LocalDate.of(1990, 1, 1);

    @BeforeEach
    void setUp() {
        repository = new InMemoryIdentityRepository();
    }

    @Test
    void savedIdentityCanBeRetrievedById() {
        DigitalID digitalID = new DigitalID("ID-1", "Test User", VALID_DOB);
        repository.save(digitalID);

        Optional<DigitalID> found = repository.findById("ID-1");

        assertTrue(found.isPresent());
    }

    @Test
    void retrievedIdentityHasCorrectId() {
        DigitalID digitalID = new DigitalID("ID-1", "Test User", VALID_DOB);
        repository.save(digitalID);

        DigitalID found = repository.findById("ID-1").orElseThrow();

        assertEquals("ID-1", found.getId());
    }

    @Test
    void retrievedIdentityHasCorrectName() {
        DigitalID digitalID = new DigitalID("ID-1", "Test User", VALID_DOB);
        repository.save(digitalID);

        DigitalID found = repository.findById("ID-1").orElseThrow();

        assertEquals("Test User", found.getFullName());
    }

    @Test
    void retrievedIdentityHasCorrectDateOfBirth() {
        DigitalID digitalID = new DigitalID("ID-1", "Test User", VALID_DOB);
        repository.save(digitalID);

        DigitalID found = repository.findById("ID-1").orElseThrow();

        assertEquals(VALID_DOB, found.getDateOfBirth());
    }

    @Test
    void retrievedIdentityHasCorrectStatus() {
        DigitalID digitalID = new DigitalID("ID-1", "Test User", VALID_DOB);
        repository.save(digitalID);

        DigitalID found = repository.findById("ID-1").orElseThrow();

        assertEquals(DigitalIDStatus.ACTIVE, found.getStatus());
    }

    @Test
    void findByIdReturnsEmptyOptionalForUnknownId() {
        Optional<DigitalID> found = repository.findById("UNKNOWN");

        assertFalse(found.isPresent());
    }

    @Test
    void findByIdReturnsEmptyOptionalOnEmptyRepository() {
        Optional<DigitalID> found = repository.findById("ID-1");

        assertFalse(found.isPresent());
    }

    @Test
    void findByIdIsCaseSensitive() {
        DigitalID digitalID = new DigitalID("ID-1", "Test User", VALID_DOB);
        repository.save(digitalID);

        Optional<DigitalID> found = repository.findById("id-1");

        assertFalse(found.isPresent());
    }

    @Test
    void existsReturnsTrueAfterSave() {
        DigitalID digitalID = new DigitalID("ID-1", "Test User", VALID_DOB);
        repository.save(digitalID);

        assertTrue(repository.exists("ID-1"));
    }

    @Test
    void existsReturnsFalseForUnknownId() {
        assertFalse(repository.exists("UNKNOWN"));
    }

    @Test
    void existsReturnsFalseOnEmptyRepository() {
        assertFalse(repository.exists("ID-1"));
    }

    @Test
    void existsIsCaseSensitive() {
        DigitalID digitalID = new DigitalID("ID-1", "Test User", VALID_DOB);
        repository.save(digitalID);

        assertFalse(repository.exists("id-1"));
    }

    @Test
    void savingIdentityWithSameIdOverwritesPrevious() {
        DigitalID original = new DigitalID("ID-1", "Original Name", VALID_DOB);
        repository.save(original);

        original.updateFullName("Updated Name");
        repository.save(original);

        DigitalID found = repository.findById("ID-1").orElseThrow();
        assertEquals("Updated Name", found.getFullName());
    }

    @Test
    void savingIdentityWithSameIdDoesNotCreateDuplicate() {
        DigitalID original = new DigitalID("ID-1", "Original Name", VALID_DOB);
        repository.save(original);
        repository.save(original);

        assertEquals(1, repository.findAll().size());
    }

    @Test
    void savedStatusChangeIsReflectedOnRetrieval() {
        DigitalID digitalID = new DigitalID("ID-1", "Test User", VALID_DOB);
        repository.save(digitalID);

        digitalID.recordStatusChange(DigitalIDStatus.SUSPENDED);
        repository.save(digitalID);

        DigitalID found = repository.findById("ID-1").orElseThrow();
        assertEquals(DigitalIDStatus.SUSPENDED, found.getStatus());
    }

    @Test
    void savedRestrictionChangeIsReflectedOnRetrieval() {
        DigitalID digitalID = new DigitalID("ID-1", "Test User", VALID_DOB);
        repository.save(digitalID);

        digitalID.setRestricted(true);
        repository.save(digitalID);

        DigitalID found = repository.findById("ID-1").orElseThrow();
        assertTrue(found.isRestricted());
    }

    @Test
    void multipleIdentitiesCanBeSavedAndRetrievedIndependently() {
        DigitalID first = new DigitalID("ID-1", "First User", VALID_DOB);
        DigitalID second = new DigitalID("ID-2", "Second User", VALID_DOB);
        DigitalID third = new DigitalID("ID-3", "Third User", VALID_DOB);

        repository.save(first);
        repository.save(second);
        repository.save(third);

        assertEquals("First User", repository.findById("ID-1").orElseThrow().getFullName());
        assertEquals("Second User", repository.findById("ID-2").orElseThrow().getFullName());
        assertEquals("Third User", repository.findById("ID-3").orElseThrow().getFullName());
    }

    @Test
    void existsReturnsTrueForEachSavedIdentity() {
        DigitalID first = new DigitalID("ID-1", "First User", VALID_DOB);
        DigitalID second = new DigitalID("ID-2", "Second User", VALID_DOB);

        repository.save(first);
        repository.save(second);

        assertTrue(repository.exists("ID-1"));
        assertTrue(repository.exists("ID-2"));
    }

    @Test
    void savingOneIdentityDoesNotAffectRetrievalOfAnother() {
        DigitalID first = new DigitalID("ID-1", "First User", VALID_DOB);
        DigitalID second = new DigitalID("ID-2", "Second User", VALID_DOB);

        repository.save(first);
        repository.save(second);

        first.updateFullName("Updated First");
        repository.save(first);

        assertEquals("Second User", repository.findById("ID-2").orElseThrow().getFullName());
    }

    @Test
    void statusHistoryIsPreservedAfterSave() {
        DigitalID digitalID = new DigitalID("ID-1", "Test User", VALID_DOB);
        digitalID.recordStatusChange(DigitalIDStatus.SUSPENDED);
        digitalID.recordStatusChange(DigitalIDStatus.ACTIVE);
        repository.save(digitalID);

        DigitalID found = repository.findById("ID-1").orElseThrow();
        assertEquals(3, found.getStatusHistory().size());
    }

    @Test
    void findAllReturnsEmptyListOnEmptyRepository() {
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void findAllReturnsAllSavedIdentities() {
        repository.save(new DigitalID("ID-1", "First User", VALID_DOB));
        repository.save(new DigitalID("ID-2", "Second User", VALID_DOB));
        repository.save(new DigitalID("ID-3", "Third User", VALID_DOB));

        assertEquals(3, repository.findAll().size());
    }

    @Test
    void findAllListIsUnmodifiable() {
        repository.save(new DigitalID("ID-1", "First User", VALID_DOB));

        assertThrows(UnsupportedOperationException.class, () ->
                repository.findAll().clear()
        );
    }
}