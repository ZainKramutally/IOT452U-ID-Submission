package com.digitalid.service;

import com.digitalid.verification.VerificationRequest;
import com.digitalid.verification.VerificationResult;

/**
 * Defines the verification capability of the Digital ID platform as a single entry point.
 * The central authority may not call this service.
 */
public interface VerificationService {

    /**
     * Verifies a Digital ID on behalf of a consuming organisation
     * The response differs by organisation type as follows:
     * TAX_AUTHORITY receives exists, valid, and a reason including suspension period checks
     * DRIVING_LICENCE_AUTHORITY receives exists, valid, and a reason including restriction checks
     * EMPLOYER and BANK receive exists, valid, and a non-null reason code, but no additional
     * detailed reason information is disclosed beyond that coarse result
     * Throws SecurityException if the organisation type is CENTRAL_AUTHORITY
     * Throws IllegalArgumentException if TAX_AUTHORITY request is missing period dates
     */
    VerificationResult verify(VerificationRequest request);
}