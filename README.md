# Digital ID System

[![CI](https://github.com/ZainKramutally/IOT452U-ID-Submission/actions/workflows/main.yml/badge.svg)](https://github.com/ZainKramutally/IOT452U-ID-Submission/actions/workflows/main.yml)

**GitHub Repository:** [https://github.com/ZainKramutally/IOT452U-ID-Submission](https://github.com/ZainKramutally/IOT452U-ID-Submission)

---

## Overview

This is a console-based backend system implementing a Digital ID platform for a federated ecosystem of organisations. The system supports two distinct capabilities: identity lifecycle management, restricted to a central authority, and identity verification, available to consuming organisations. These two capabilities are intentionally separated into independent services, reflecting the core architectural decision of the system.

The central authority is the only actor permitted to create, update, and manage the status of Digital IDs. Consuming organisations, including tax authorities, driving licence authorities, employers, and banks, interact with the system solely through verification requests. Each organisation type receives a response shaped to its permitted level of disclosure. Organisations do not communicate with one another and all requests flow through the shared platform.

Every key action in the system, including rejected operations, is recorded to an audit log so that system behaviour can be examined at any point.

---

## Requirements

| Technology | Version |
|---|---|
| Java | 17+ |
| Maven | 3.8+ |

Verify your installation:

```sh
java -version
mvn -version
```

---

## Quick Start

Clone the repository:

```sh
git clone https://github.com/ZainKramutally/IOT452U-ID-Submission.git
cd IOT452U-ID-Submission
```

Run all tests:

```sh
mvn test
```

Run the full build including coverage report:

```sh
mvn verify
```

Run the console demonstration:

```sh
mvn compile
java -cp target/classes com.digitalid.Main
```

View the JaCoCo coverage report by opening `target/site/jacoco/index.html` in a browser after running `mvn verify`.

---

## Example Demonstration Output

```text
============================================================
  SECTION 1 : IDENTITY CREATION
============================================================

Creating three Digital IDs as the central authority...

Created: ID-1001 | Alice Johnson | DOB: 1990-03-15
Created: ID-1002 | Bob Patel    | DOB: 1985-07-22
Created: ID-1003 | Carol Smith  | DOB: 1978-11-08

Attempting to create a duplicate ID (ID-1001)...
Rejected as expected: Digital ID already exists: ID-1001

Attempting to create an identity as an EMPLOYER (unauthorised)...
Rejected as expected: Only the central authority may perform this action
```

---

## System Structure

```text
src/
├── main/java/com/digitalid/
│   ├── audit/
│   │   ├── AuditActions.java
│   │   ├── AuditDetailKeys.java
│   │   ├── AuditEvent.java
│   │   ├── AuditLog.java
│   │   └── AuditReasons.java
│   ├── domain/
│   │   ├── DigitalID.java
│   │   ├── DigitalIDStatus.java
│   │   ├── OrganisationType.java
│   │   ├── ReasonCode.java
│   │   ├── RestrictionChange.java
│   │   └── StatusChange.java
│   ├── repository/
│   │   ├── IdentityRepository.java
│   │   └── InMemoryIdentityRepository.java
│   ├── service/
│   │   ├── ManagementService.java
│   │   ├── ManagementServiceImpl.java
│   │   ├── VerificationService.java
│   │   └── VerificationServiceImpl.java
│   ├── verification/
│   │   ├── VerificationRequest.java
│   │   └── VerificationResult.java
│   └── Main.java
└── test/java/com/digitalid/
    ├── domain/
    │   └── DigitalIDTest.java
    ├── repository/
    │   └── IdentityRepositoryTest.java
    └── service/
        ├── ManagementServiceTest.java
        └── VerificationServiceTest.java
```

### Package Responsibilities

**`audit`** records all key system actions. `AuditEvent` is an immutable record storing a timestamp, action label, and detail string. `AuditLog` holds an ordered list of events and exposes a `printAll` method for demonstration. `AuditActions` defines all action label constants. `AuditDetailKeys` defines all audit detail field name constants such as id, from, to, reason, and org. `AuditReasons` defines all rejection reason constants such as UNAUTHORISED, REVOKED, DUPLICATE, and MISSING_ID. Together these three classes mean the entire audit system is free of magic strings.

**`domain`** is the core model with no dependencies on any other package. `DigitalID` holds both immutable fields (`id`, `dateOfBirth`) and mutable fields (`fullName`, `status`). It maintains a full `statusHistory` as a list of `StatusChange` records and a `restrictionHistory` as a list of `RestrictionChange` records. `DigitalIDStatus` is an enum that encodes valid state transitions directly on each constant via `canTransitionTo`. `OrganisationType` and `ReasonCode` are enums that eliminate magic strings from the service and verification layers.

**`repository`** defines the `IdentityRepository` interface and provides an `InMemoryIdentityRepository` implementation backed by a `ConcurrentHashMap`. The service layer depends only on the interface, meaning the storage mechanism can be replaced without touching any business logic.

**`service`** contains the two distinct system capabilities. `ManagementService` and its implementation handle all identity lifecycle operations and are restricted to the central authority. `VerificationService` and its implementation handle all verification requests from consuming organisations and shape the response according to organisation type.

**`verification`** defines `VerificationRequest` and `VerificationResult` as records. `VerificationRequest` carries the digital ID, organisation type, and optional period dates for tax authority requests. `VerificationResult` carries an exists flag, a valid flag, a `ReasonCode`, and a nullable detail field that is intentionally withheld for employer and bank responses to enforce limited disclosure in code.

---

## Identity Status Lifecycle

The system supports three identity states with the following permitted transitions:

| From | To | Permitted |
|---|---|---|
| ACTIVE | SUSPENDED | Yes |
| ACTIVE | REVOKED | Yes |
| SUSPENDED | ACTIVE | Yes |
| SUSPENDED | REVOKED | Yes |
| REVOKED | Any | No |

REVOKED is a terminal state. Invalid transitions are rejected with an `IllegalStateException` and recorded in the audit log. Repeated operations with the same status are handled as a no-op without adding a history entry.

---

## Verification by Organisation Type

| Organisation | Checks | Detail Exposed |
|---|---|---|
| EMPLOYER | Exists, ACTIVE | None |
| BANK | Exists, ACTIVE | None |
| DRIVING_LICENCE_AUTHORITY | Exists, ACTIVE, not restricted | ReasonCode |
| TAX_AUTHORITY | Exists, ACTIVE, not suspended during period | ReasonCode |
| CENTRAL_AUTHORITY | Not permitted | SecurityException |

---

## Key Design Decisions

### Management and Verification as Separate Capabilities

The brief explicitly states that identity management and identity consumption must be treated as distinct system capabilities. This is reflected in two separate service interfaces with no shared state or inheritance. `ManagementService` is the only entry point for write operations. `VerificationService` is the only entry point for read operations. A consuming organisation calling `ManagementService` receives a `SecurityException`. The central authority calling `VerificationService` also receives a `SecurityException`. Neither capability is accessible through the other.

### Status Transitions on the Enum

Valid status transitions are defined directly on `DigitalIDStatus` via an abstract `canTransitionTo` method implemented by each constant. This means the transition rules live in exactly one place and the service layer does not need to know what transitions are valid, it simply asks the current status. If a new status value is added in future, the compiler enforces that its transitions are explicitly defined. The no-op case where the same status is requested again is handled separately in the service as a deliberate design decision: `canTransitionTo` models genuine state changes only, keeping its meaning precise.

### Status History for Period-Based Verification

`DigitalID` records every status change as a timestamped `StatusChange` entry rather than storing only the current status. This is required for the tax authority period check, which must determine whether an identity was suspended at any point during a reporting period, not just whether it is currently suspended. The verification service converts the history into intervals and checks for overlap with the requested period, correctly handling cases where a suspension started before the period began, ended after it started, or was reinstated before the period but still overlapped it.

### Restriction History with Expiry

Restrictions are stored as a `RestrictionChange` history rather than a single boolean flag. Each restriction entry carries a reason and a timestamp; the management service requires an expiry date whenever a restriction is applied. The domain model allows a null expiry for low-level usage (for example, in tests or direct domain manipulation), but the service layer enforces the expiry rule for all managed operations. `isRestricted()` on `DigitalID` evaluates the most recent restriction entry and returns false once that expiry date has passed. This allows time-limited restrictions to expire automatically without requiring a manual update from the central authority. The history is preserved in full so past restrictions remain auditable even after they have expired.

### Audit Logging of Rejections

Every operation that is rejected, whether due to an unauthorised actor, an invalid status transition, a revoked identity, or a missing required field, produces an audit record before the exception is thrown. This means the audit log represents a complete history of all attempted actions, not just successful ones. The `AuditActions` constants class ensures all action labels are consistent and defined in a single place.

### No Magic Strings

All reason codes are defined in the `ReasonCode` enum. All audit action labels are defined as constants in `AuditActions`. All audit detail field name keys such as id, from, to, org, and reason are defined in `AuditDetailKeys`. All rejection reason strings such as UNAUTHORISED, REVOKED, DUPLICATE, and MISSING_ID are defined in `AuditReasons`. The codebase contains no inline string literals used as identifiers anywhere in the service or verification layers. Every string that carries meaning is defined once and referenced everywhere.

### Java Records for Value Objects

Immutable value objects throughout the system — `AuditEvent`, `StatusChange`, `RestrictionChange`, `VerificationRequest`, and `VerificationResult` — are implemented as Java records. Records eliminate constructor, getter, equals, hashCode, and toString boilerplate whilst making immutability explicit by design. This is idiomatic Java 17 and keeps value-carrying classes concise and readable.

### Programming to Interfaces

Both service classes are defined as interfaces with separate implementation classes. The service layer accepts `IdentityRepository` by interface rather than the concrete `InMemoryIdentityRepository`. This means any component can be substituted, for example replacing the in-memory store with a database-backed implementation, without changing any business logic. It also makes each component independently testable.

---

## Continuous Integration

The project uses GitHub Actions. The pipeline triggers on every push to every branch and on every pull request, ensuring CI activity is visible throughout the full development history.

The pipeline performs the following steps on each run:

- Checks out the code
- Sets up JDK 17 using the Temurin distribution
- Runs `mvn verify` which compiles, executes all tests, and generates the JaCoCo coverage report
- Uploads Surefire test reports as a build artefact
- Uploads the JaCoCo coverage report as a build artefact

The pipeline fails if any test fails, ensuring no broken code can be merged without the failure being visible in the Actions tab.

---

## Test Coverage

The test suite covers the following areas across four test classes:

- Domain model construction, immutability, and validation
- Status history and restriction history behaviour
- Repository save, retrieval, overwrite, and case sensitivity
- Management service happy paths and all rejection scenarios
- Audit records for both successful and rejected operations
- Verification responses for all five organisation types
- Tax authority suspension period overlap detection including boundary cases
- Timezone-safe date handling using UTC consistently

Tests are written using JUnit 5 with `@BeforeEach` setup and descriptive method names that clearly state what each test verifies.

---

## Architecture Diagram

```text
                       +----------------------+
                       |  Central Authority   |
                       +----------+-----------+
                                  |
                                  v
                        +-------------------+
                        | ManagementService |
                        +---------+---------+
                                  |
                 +----------------+----------------+
                 |                                 |
                 v                                 v
      +----------------------+          +-------------------+
      | IdentityRepository   |          |     AuditLog      |
      +----------+-----------+          +-------------------+
                 |
                 v
          +-------------+
          | DigitalID   |
          +-------------+

                       +----------------------+
                       | Consuming Orgs       |
                       | (Employer/Bank/etc.) |
                       +----------+-----------+
                                  |
                                  v
                        +-------------------+
                        | VerificationService|
                        +---------+---------+
                                  |
                 +----------------+----------------+
                 |                                 |
                 v                                 v
      +----------------------+          +-------------------+
      | IdentityRepository   |          |     AuditLog      |
      +----------------------+          +-------------------+
```
