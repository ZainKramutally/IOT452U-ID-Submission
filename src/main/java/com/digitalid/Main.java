package com.digitalid;

import com.digitalid.audit.AuditLog;
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
import java.time.ZoneOffset;

public class Main {

    private static final LocalDate TODAY_UTC = LocalDate.now(ZoneOffset.UTC);

    public static void main(String[] args) {
        InMemoryIdentityRepository repository = new InMemoryIdentityRepository();
        AuditLog auditLog = new AuditLog();

        ManagementService management = new ManagementServiceImpl(repository, auditLog);
        VerificationService verification = new VerificationServiceImpl(repository, auditLog);

        // SECTION 1 : IDENTITY CREATION
        printHeader("SECTION 1 : IDENTITY CREATION");

        System.out.println("Creating three Digital IDs as the central authority...\n");

        management.createIdentity("ID-1001", "Alice Johnson",
                LocalDate.of(1990, 3, 15), OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("Created: ID-1001 | Alice Johnson | DOB: 1990-03-15");

        management.createIdentity("ID-1002", "Bob Patel",
                LocalDate.of(1985, 7, 22), OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("Created: ID-1002 | Bob Patel | DOB: 1985-07-22");

        management.createIdentity("ID-1003", "Carol Smith",
                LocalDate.of(1978, 11, 8), OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("Created: ID-1003 | Carol Smith | DOB: 1978-11-08");

        System.out.println("\nAttempting to create a duplicate ID (ID-1001)...");
        try {
            management.createIdentity("ID-1001", "Duplicate Name",
                    LocalDate.of(1990, 3, 15), OrganisationType.CENTRAL_AUTHORITY);
        } catch (IllegalStateException e) {
            System.out.println("Rejected as expected: " + e.getMessage());
        }

        System.out.println("\nAttempting to create an identity as an EMPLOYER (unauthorised)...");
        try {
            management.createIdentity("ID-9999", "Unauthorised Name",
                    LocalDate.of(2000, 1, 1), OrganisationType.EMPLOYER);
        } catch (SecurityException e) {
            System.out.println("Rejected as expected: " + e.getMessage());
        }

        // SECTION 2 : IDENTITY UPDATES
        printHeader("SECTION 2 : IDENTITY UPDATES");

        System.out.println("Updating name for ID-1001 from 'Alice Johnson' to 'Alice Williams'...");
        management.updateName("ID-1001", "Alice Williams", OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("Name updated. New name: "
                + repository.findById("ID-1001").orElseThrow().getFullName());

        // SECTION 3 : STATUS MANAGEMENT
        printHeader("SECTION 3 : STATUS MANAGEMENT");

        System.out.println("Suspending ID-1002 (Bob Patel)...");
        management.changeStatus("ID-1002", DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("Status: " + repository.findById("ID-1002").orElseThrow().getStatus());

        System.out.println("\nReinstating ID-1002 back to ACTIVE...");
        management.changeStatus("ID-1002", DigitalIDStatus.ACTIVE, OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("Status: " + repository.findById("ID-1002").orElseThrow().getStatus());

        System.out.println("\nRevoking ID-1003 (Carol Smith)...");
        management.changeStatus("ID-1003", DigitalIDStatus.REVOKED, OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("Status: " + repository.findById("ID-1003").orElseThrow().getStatus());

        System.out.println("\nAttempting to update name on revoked ID-1003...");
        try {
            management.updateName("ID-1003", "Carol Jones", OrganisationType.CENTRAL_AUTHORITY);
        } catch (IllegalStateException e) {
            System.out.println("Rejected as expected: " + e.getMessage());
        }

        System.out.println("\nAttempting invalid status transition (REVOKED -> ACTIVE) on ID-1003...");
        try {
            management.changeStatus("ID-1003", DigitalIDStatus.ACTIVE, OrganisationType.CENTRAL_AUTHORITY);
        } catch (IllegalStateException e) {
            System.out.println("Rejected as expected: " + e.getMessage());
        }

        System.out.println("\nApplying no-op status change (ACTIVE -> ACTIVE) on ID-1001...");
        management.changeStatus("ID-1001", DigitalIDStatus.ACTIVE, OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("Handled gracefully. Status unchanged: "
                + repository.findById("ID-1001").orElseThrow().getStatus());

        // SECTION 4 : RESTRICTION MANAGEMENT
        printHeader("SECTION 4 : RESTRICTION MANAGEMENT");

        System.out.println("Setting restriction flag on ID-1002 (Bob Patel)...");
        management.setRestricted("ID-1002", true, OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("Restricted: " + repository.findById("ID-1002").orElseThrow().isRestricted());

        // SECTION 5 : VERIFICATION: EMPLOYER AND BANK
        printHeader("SECTION 5 : VERIFICATION: EMPLOYER AND BANK");

        System.out.println("EMPLOYER verifying active identity ID-1001 (Alice Williams)...");
        printResult(verification.verify(
                new VerificationRequest("ID-1001", OrganisationType.EMPLOYER, null, null)
        ));

        System.out.println("BANK verifying active identity ID-1001 (Alice Williams)...");
        printResult(verification.verify(
                new VerificationRequest("ID-1001", OrganisationType.BANK, null, null)
        ));

        System.out.println("EMPLOYER verifying restricted identity ID-1002 (Bob Patel)...");
        System.out.println("(Restriction is irrelevant to employer : should return valid)");
        printResult(verification.verify(
                new VerificationRequest("ID-1002", OrganisationType.EMPLOYER, null, null)
        ));

        System.out.println("EMPLOYER verifying revoked identity ID-1003 (Carol Smith)...");
        printResult(verification.verify(
                new VerificationRequest("ID-1003", OrganisationType.EMPLOYER, null, null)
        ));

        // SECTION 6 : VERIFICATION: DRIVING LICENCE AUTHORITY
        printHeader("SECTION 6 : VERIFICATION: DRIVING LICENCE AUTHORITY");

        System.out.println("DRIVING_LICENCE_AUTHORITY verifying active unrestricted ID-1001...");
        printResult(verification.verify(
                new VerificationRequest("ID-1001", OrganisationType.DRIVING_LICENCE_AUTHORITY, null, null)
        ));

        System.out.println("DRIVING_LICENCE_AUTHORITY verifying active restricted ID-1002 (Bob Patel)...");
        printResult(verification.verify(
                new VerificationRequest("ID-1002", OrganisationType.DRIVING_LICENCE_AUTHORITY, null, null)
        ));

        // SECTION 7 : VERIFICATION: TAX AUTHORITY
        printHeader("SECTION 7 : VERIFICATION: TAX AUTHORITY");

        // Set up a dedicated identity for tax authority period check scenarios
        management.createIdentity("ID-2001", "David Lee",
                LocalDate.of(1982, 4, 10), OrganisationType.CENTRAL_AUTHORITY);

        System.out.println("TAX_AUTHORITY verifying active identity ID-2001 with no suspensions in period...");
        printResult(verification.verify(
                new VerificationRequest("ID-2001", OrganisationType.TAX_AUTHORITY,
                        TODAY_UTC.minusDays(30), TODAY_UTC.plusDays(30))
        ));

        // Suspend and reinstate so the suspension falls within the reporting period
        management.changeStatus("ID-2001", DigitalIDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);
        management.changeStatus("ID-2001", DigitalIDStatus.ACTIVE, OrganisationType.CENTRAL_AUTHORITY);

        System.out.println("ID-2001 was suspended and reinstated within the current period.");
        System.out.println("TAX_AUTHORITY verifying : period overlaps with the suspension...");
        printResult(verification.verify(
                new VerificationRequest("ID-2001", OrganisationType.TAX_AUTHORITY,
                        TODAY_UTC.minusDays(1), TODAY_UTC.plusDays(1))
        ));

        System.out.println("TAX_AUTHORITY verifying : period is entirely in the future (no overlap)...");
        printResult(verification.verify(
                new VerificationRequest("ID-2001", OrganisationType.TAX_AUTHORITY,
                        TODAY_UTC.plusYears(1), TODAY_UTC.plusYears(2))
        ));

        // SECTION 8 : VERIFICATION: NOT FOUND
        printHeader("SECTION 8 : VERIFICATION: NOT FOUND");

        System.out.println("EMPLOYER verifying non-existent ID (ID-XXXX)...");
        printResult(verification.verify(
                new VerificationRequest("ID-XXXX", OrganisationType.EMPLOYER, null, null)
        ));

        // SECTION 9: CENTRAL AUTHORITY VERIFICATION REJECTION
        printHeader("SECTION 9 : CENTRAL AUTHORITY VERIFICATION REJECTION");

        System.out.println("CENTRAL_AUTHORITY attempting to call verify (not permitted)...");
        try {
            verification.verify(
                    new VerificationRequest("ID-1001", OrganisationType.CENTRAL_AUTHORITY, null, null)
            );
        } catch (SecurityException e) {
            System.out.println("Rejected as expected: " + e.getMessage());
        }

        // SECTION 10 : FULL AUDIT LOG
        printHeader("SECTION 10 : FULL AUDIT LOG");

        System.out.println("Every action recorded throughout this demonstration:\n");
        auditLog.printAll();
    }

    private static void printHeader(String title) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("  " + title);
        System.out.println("=".repeat(60) + "\n");
    }

    private static void printResult(VerificationResult result) {
        System.out.println("  exists=" + result.exists()
                + " | valid=" + result.isValid()
                + " | reason=" + result.getReason()
                + (result.getDetail() != null ? " | detail=" + result.getDetail() : ""));
        System.out.println();
    }
}