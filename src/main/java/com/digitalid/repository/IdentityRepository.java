package com.digitalid.repository;

import com.digitalid.domain.DigitalID;

import java.util.Optional;
import java.util.List;

/**
 * Contract for storing and retrieving DigitalID entities.
 */
public interface IdentityRepository {
    /**
     * Returns an unmodifiable snapshot of all Digital IDs currently held in the store.
     * Changes to the repository after this method returns are not reflected in the returned list.
     * No specific iteration order is guaranteed.
     */
    List<DigitalID> findAll();


    /**
     * Persists a Digital ID
     * If exists it is overwritten with the provided instance
     */
    void save(DigitalID digitalID);

    /**
     * Returns the Digital ID with the given ID, or an empty Optional
     */
    Optional<DigitalID> findById(String id);

    /**
     * Returns true if a Digital ID with the given ID exists in the store.
     */
    boolean exists(String id);
}

