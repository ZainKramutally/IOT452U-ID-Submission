package com.digitalid.repository;

import com.digitalid.domain.DigitalID;

import java.util.Optional;

public interface IdentityRepository {
    Optional<DigitalID> findById(String id);

    void save(DigitalID digitalID);

    boolean exists(String id);
}

