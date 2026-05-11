package com.digitalid.service;

import com.digitalid.domain.DigitalID;
import com.digitalid.domain.DigitalIDStatus;
import com.digitalid.domain.OrganisationType;

import java.time.LocalDate;

/**
 * Defines the management capability so all operations are restricted to the central authority.
 */
public interface ManagementService {

    /**
     * Creates a new Digital ID with ACTIVE status.
     * Throws SecurityException if the actor is not CENTRAL_AUTHORITY.
     * Throws IllegalStateException if a Digital ID with the given ID already exists.
     */
    DigitalID createIdentity(String id, String fullName, LocalDate dateOfBirth, OrganisationType actor);

    /**
     * Updates the full name of an existing Digital ID.
     * Throws SecurityException if the actor is not CENTRAL_AUTHORITY.
     * Throws IllegalArgumentException if the ID does not exist.
     * Throws IllegalStateException if the Digital ID is REVOKED.
     */
    void updateName(String id, String fullName, OrganisationType actor);

    /**
     * Changes the status of an existing Digital ID.
     * Validates the transition using DigitalIDStatus.canTransitionTo.
     * Same-status requests are handled as a no-op without recording a history entry.
     * Throws SecurityException if the actor is not CENTRAL_AUTHORITY.
     * Throws IllegalArgumentException if the ID does not exist.
     * Throws IllegalStateException if the transition is not permitted.
     */
    void changeStatus(String id, DigitalIDStatus status, OrganisationType actor);

    /**
     * Sets or removes the restriction flag on an existing Digital ID.
     * Throws SecurityException if the actor is not CENTRAL_AUTHORITY.
     * Throws IllegalArgumentException if the ID does not exist.
     * Throws IllegalStateException if the Digital ID is REVOKED.
     */
    void setRestricted(String id, boolean restricted, OrganisationType actor);
}