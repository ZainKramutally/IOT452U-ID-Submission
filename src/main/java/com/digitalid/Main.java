package com.digitalid;

import com.digitalid.audit.AuditLog;
import com.digitalid.domain.DigitalID;
import com.digitalid.domain.DigitalIDStatus;
import com.digitalid.domain.OrganisationType;
import com.digitalid.repository.InMemoryIdentityRepository;
import com.digitalid.service.ManagementService;
import com.digitalid.service.ManagementServiceImpl;
import com.digitalid.service.VerificationService;
import com.digitalid.service.VerificationServiceImpl;
import com.digitalid.verification.VerificationRequest;
import com.digitalid.verification.VerificationResult;

import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {

        // TEST FOR CI PIPELINE
        InMemoryIdentityRepository repository = new InMemoryIdentityRepository();
        AuditLog auditLog = new AuditLog();

        ManagementService managementService = new ManagementServiceImpl(repository, auditLog);
        VerificationService verificationService = new VerificationServiceImpl(repository, auditLog);

        DigitalID digitalID = managementService.createIdentity(
                "ID-1001",
                "Alex Morgan",
                LocalDate.of(1995, 7, 12),
                OrganisationType.CENTRAL_AUTHORITY
        );

        managementService.changeStatus(digitalID.getId(), DigitalIDStatus.ACTIVE, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult result = verificationService.verify(
                new VerificationRequest(digitalID.getId(), OrganisationType.EMPLOYER, LocalDate.now())
        );

        System.out.println("\n" + result);
    }
}

