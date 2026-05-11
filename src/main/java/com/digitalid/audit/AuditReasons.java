package com.digitalid.audit;

public final class AuditReasons {
    public static final String UNAUTHORISED = "UNAUTHORISED";
    public static final String DUPLICATE = "DUPLICATE";
    public static final String REVOKED = "REVOKED";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String MISSING_REQUEST = "MISSING_REQUEST";
    public static final String MISSING_ACTOR = "MISSING_ACTOR";
    public static final String MISSING_ID = "MISSING_ID";
    public static final String MISSING_ORG = "MISSING_ORG";
    public static final String MISSING_FULL_NAME = "MISSING_FULL_NAME";
    public static final String MISSING_DATE_OF_BIRTH = "MISSING_DATE_OF_BIRTH";
    public static final String MISSING_STATUS = "MISSING_STATUS";
    public static final String MISSING_REASON = "MISSING_REASON";
    public static final String MISSING_EXPIRES_ON = "MISSING_EXPIRES_ON";
    public static final String MISSING_PERIODS = "MISSING_PERIODS";
    public static final String MISSING_PERIOD_START = "MISSING_PERIOD_START";
    public static final String MISSING_PERIOD_END = "MISSING_PERIOD_END";

    private AuditReasons() {
    }
}
