package com.digitalid.repository;

import com.digitalid.domain.DigitalID;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryIdentityRepository implements IdentityRepository {
    private final Map<String, DigitalID> store = new ConcurrentHashMap<>();

    @Override
    public Optional<DigitalID> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public void save(DigitalID digitalID) {
        store.put(digitalID.getId(), digitalID);
    }

    @Override
    public boolean exists(String id) {
        return store.containsKey(id);
    }

    @Override
    public List<DigitalID> findAll() {
        return List.copyOf(store.values());
    }
}

