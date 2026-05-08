package com.digitalid.service;

import com.digitalid.domain.DigitalID;
import com.digitalid.domain.DigitalIDStatus;
import com.digitalid.domain.OrganisationType;

import java.time.LocalDate;

public interface ManagementService {
    DigitalID createIdentity(String id, String fullName, LocalDate dateOfBirth, OrganisationType actor);

    DigitalID updateName(String id, String fullName, OrganisationType actor);

    DigitalID changeStatus(String id, DigitalIDStatus status, OrganisationType actor);

    DigitalID setRestricted(String id, boolean restricted, OrganisationType actor);
}

