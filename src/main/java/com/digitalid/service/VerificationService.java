package com.digitalid.service;

import com.digitalid.verification.VerificationRequest;
import com.digitalid.verification.VerificationResult;

public interface VerificationService {
    VerificationResult verify(VerificationRequest request);
}

