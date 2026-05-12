# Digital ID System

[![CI](https://github.com/ZainKramutally/IOT452U-ID-Submission/actions/workflows/main.yml/badge.svg)](https://github.com/ZainKramutally/IOT452U-ID-Submission/actions/workflows/main.yml)

**GitHub Repository:** https://github.com/ZainKramutally/IOT452U-ID-Submission

---

## Overview

This project is a console-based backend system implementing a Digital ID platform for a federated ecosystem of organisations for IOT453U QMUL Assignment. The implementation is in Java 17 and focuses on system behaviour, clean structure, and sound engineering practice rather than a user interface.

The system is organised around distinct capabilities. Identity lifecycle management, creating identities, updating names, managing status, and applying restrictions, is handled exclusively by a central authority. Identity verification is handled separately and made available to consuming organisations including tax authorities, driving licence authorities, employers, and banks. Each organisation type receives a response appropriate to its permitted level of disclosure. These two capabilities are intentionally separated at the architectural level and neither is accessible through the other.

All significant system actions, including rejected operations, are recorded to an audit log so that system behaviour can be examined at any point.

---

## Requirements

- Java 17+
- Maven 3.8+

Verify your setup before running:

```sh
java -version
mvn -version
```

---

## Running the System

Clone the repository:

```sh
git clone https://github.com/ZainKramutally/IOT452U-ID-Submission.git
cd IOT452U-ID-Submission
```

Run all tests:

```sh
mvn test
```

Run the full build including the JaCoCo coverage report:

```sh
mvn verify
```

Run the console demonstration:

```sh
mvn compile
java -cp target/classes com.digitalid.Main
```

The coverage report is generated at `target/site/jacoco/index.html` after running `mvn verify` and shows the coverage for each branch and etc.

---

## Project Structure

```text
src/
├── main/java/com/digitalid/
│   ├── audit/
│   │   ├── AuditActions.java         ← action label constants
│   │   ├── AuditDetailKeys.java      ← detail field key constants
│   │   ├── AuditEvent.java           ← immutable record: timestamp, action, details
│   │   ├── AuditLog.java             ← ordered event list with printAll()
│   │   └── AuditReasons.java         ← rejection reason string constants
│   ├── domain/
│   │   ├── DigitalID.java            ← core model with status and restriction history
│   │   ├── DigitalIDStatus.java      ← enum with canTransitionTo() per constant
│   │   ├── OrganisationType.java     ← all participating organisation types
│   │   ├── ReasonCode.java           ← all verification outcome codes
│   │   ├── RestrictionChange.java    ← record: restricted, reason, expiresOn, timestamp
│   │   └── StatusChange.java         ← record: status, timestamp
│   ├── repository/
│   │   ├── IdentityRepository.java         ← storage interface
│   │   └── InMemoryIdentityRepository.java ← ConcurrentHashMap implementation
│   ├── service/
│   │   ├── ManagementService.java          ← management capability interface
│   │   ├── ManagementServiceImpl.java      ← lifecycle rules and business logic
│   │   ├── VerificationService.java        ← verification capability interface
│   │   └── VerificationServiceImpl.java    ← organisation-differentiated verification
│   ├── verification/
│   │   ├── VerificationRequest.java  ← record: digitalId, orgType, periodStart, periodEnd
│   │   └── VerificationResult.java   ← record: exists, valid, reason, detail
│   └── Main.java                     ← scripted console demonstration
└── test/java/com/digitalid/
    ├── domain/DigitalIDTest.java
    ├── repository/IdentityRepositoryTest.java
    └── service/
        ├── ManagementServiceTest.java
        └── VerificationServiceTest.java
```

---

## Architecture

Both services share a single `IdentityRepository` and a single `AuditLog`. There is one in-memory store and one audit trail for the whole system.

```text
  +---------------------+              +-------------------------+
  |  Central Authority  |              |  Consuming Organisation |
  |                     |              |  Employer / Bank /      |
  |                     |              |  Driving Licence /      |
  |                     |              |  Tax Authority          |
  +----------+----------+              +------------+------------+
             |                                      |
             v                                      v
  +----------+----------+              +------------+------------+
  |  ManagementService  |              |  VerificationService    |
  +----------+----------+              +------------+------------+
             |                                      |
             +------------------+-------------------+
                                |
               +----------------+----------------+
               |                                 |
               v                                 v
    +----------+----------+           +----------+----------+
    | IdentityRepository  |           |      AuditLog       |
    +----------+----------+           +---------------------+
               |
               v
    +----------+----------+
    |      DigitalID      |
    +---------------------+
```

---

## Status Transitions

| From | To | Permitted |
|---|---|---|
| ACTIVE | SUSPENDED | Yes |
| ACTIVE | REVOKED | Yes |
| SUSPENDED | ACTIVE | Yes |
| SUSPENDED | REVOKED | Yes |
| REVOKED | Anything | No |

REVOKED is a terminal state. Requesting the same status that is already set is handled gracefully as a no-op, logged as `CHANGE_STATUS_NO_OP`, and does not add an entry to the status history. All invalid transitions throw an `IllegalStateException` and are recorded in the audit log.

---

## Verification Behaviour by Organisation Type

| Organisation | Checks Performed | Response |
|---|---|---|
| EMPLOYER | Exists, ACTIVE | valid true/false, no detail |
| BANK | Exists, ACTIVE | valid true/false, no detail |
| DRIVING_LICENCE_AUTHORITY | Exists, ACTIVE, not restricted | valid + ReasonCode |
| TAX_AUTHORITY | Exists, ACTIVE, not suspended during period | valid + ReasonCode |
| CENTRAL_AUTHORITY | Not permitted | SecurityException |

---

## Key Design Decisions

### Separation of Management and Verification

The project brief requires these to be treated as distinct capabilities, and this is reflected directly in the structure. `ManagementService` is the sole entry point for all write operations. `VerificationService` is the sole entry point for all read operations. A consuming organisation attempting to call `ManagementService` receives a `SecurityException`. The central authority attempting to call `VerificationService` receives the same. The separation is enforced at runtime, not by convention.

### Transition Logic on the Enum

Each `DigitalIDStatus` constant implements its own `canTransitionTo` method. This keeps all transition rules in one place. The service layer does not need to know what is or is not a valid transition,  it simply asks the current status. A practical benefit of this approach is that if a new status value is introduced in future, the compiler requires its transition rules to be explicitly defined before the code will build.

The no-op case, where the same status is requested again, is handled separately in the service rather than in `canTransitionTo`. This keeps the method's meaning precise: it only describes genuine state changes.

### Status History for Period-Based Verification

Storing only the current status would make the tax authority period check impossible to implement correctly. Instead, every status change is recorded as a `StatusChange` entry with a UTC timestamp. The verification service reconstructs each status interval from the history and tests it for overlap against the requested reporting period. This approach correctly handles cases where a suspension started before the period began, ended during it, or spans the entire period, none of which a point-in-time status check could detect, which is what I had initially and produced bugs.

### Restriction History with Automatic Expiry

Restrictions are stored as a `RestrictionChange` history rather than a single boolean flag. Each entry carries a reason, an optional expiry date, and a timestamp. `isRestricted()` on `DigitalID` evaluates the most recent entry and returns false once its expiry date has passed. This allows time-limited restrictions to expire without requiring a manual update from the central authority. The full history is preserved so that past restrictions remain auditable after they have expired.

### Audit Logging of Rejections

Writing an audit record only on success would produce an incomplete log. Every rejected operation, unauthorised actor, invalid status transition, revoked identity, missing field, produces an audit record before the exception is thrown. The log therefore reflects everything the system received and attempted, not just what succeeded.

### No Magic Strings

`AuditActions` defines all action label constants. `AuditDetailKeys` defines all audit detail field key names. `AuditReasons` defines all rejection reason strings. `ReasonCode` is an enum for all verification outcomes. No meaningful string literal appears inline anywhere in the service or verification layers,  every identifier is defined once and referenced wherever it is needed.

### Java Records for Value Objects

`AuditEvent`, `StatusChange`, `RestrictionChange`, `VerificationRequest`, and `VerificationResult` are implemented as Java records. Records enforce immutability by design and eliminate the need for manually written constructors, getters, `equals`, `hashCode`, and `toString`. This is the idiomatic Java 17 approach for objects whose sole purpose is to carry data.

### Programming to Interfaces

Both service implementations and the repository accept dependencies through interfaces rather than concrete types. The service layer references `IdentityRepository`, not `InMemoryIdentityRepository`. This means the storage layer could be replaced with a database-backed implementation without modifying any business logic. It also allows each component to be tested in isolation.

---

## Continuous Integration

The project uses GitHub Actions. The pipeline is configured to trigger on every push and pull request across all branches, so CI activity is visible throughout the development history rather than appearing only at the end.

Each run sets up JDK 17 using the Temurin distribution, executes `mvn verify` to compile, test, and generate the coverage report, and uploads both the Surefire test reports and JaCoCo coverage report as build artefacts. A failing test fails the pipeline.

---

## Test Coverage

The test suite spans four classes and is written with JUnit 5. Each test method is named to describe exactly what it is asserting.

**`DigitalIDTest`** covers domain model construction, immutable field enforcement, blank and null input validation, status history behaviour, restriction history with expiry logic, and the transition rules on `DigitalIDStatus`.

**`IdentityRepositoryTest`** covers save and retrieval, overwrite behaviour, case sensitivity, empty repository handling, and `findAll`.

**`ManagementServiceTest`** covers every happy path and every rejection scenario across all four operations, including audit records produced by both successful and failed operations.

**`VerificationServiceTest`** covers all five organisation types, tax authority period overlap detection including boundary cases, and UTC-consistent date handling to prevent timezone-related failures.