package com.digitalid.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DigitalID {
    private final String id;
    private final LocalDate dateOfBirth;
    private String fullName;
    private DigitalIDStatus status;
    private boolean restricted;
    private final List<StatusChange> statusHistory = new ArrayList<>();

    public DigitalID(String id, String fullName, LocalDate dateOfBirth) {
        this.id = requireText(id, "id");
        this.fullName = requireText(fullName, "fullName");
        this.dateOfBirth = Objects.requireNonNull(dateOfBirth, "dateOfBirth");
        this.status = DigitalIDStatus.ACTIVE;
        this.restricted = false;
        statusHistory.add(new StatusChange(DigitalIDStatus.ACTIVE, Instant.now()));
    }

    public String getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public DigitalIDStatus getStatus() {
        return status;
    }

    public boolean isRestricted() {
        return restricted;
    }

    public List<StatusChange> getStatusHistory() {
        return Collections.unmodifiableList(statusHistory);
    }

    public void updateFullName(String fullName) {
        this.fullName = requireText(fullName, "fullName");
    }

    public void recordStatusChange(DigitalIDStatus newStatus) {
        this.status = Objects.requireNonNull(newStatus, "status");
        statusHistory.add(new StatusChange(newStatus, Instant.now()));
    }

    public void setRestricted(boolean restricted) {
        this.restricted = restricted;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return trimmed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DigitalID digitalID = (DigitalID) o;
        return id.equals(digitalID.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}