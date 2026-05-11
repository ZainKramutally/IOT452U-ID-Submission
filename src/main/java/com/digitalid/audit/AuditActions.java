package com.digitalid.audit;

public final class AuditActions {
    public static final String CREATE_IDENTITY = "CREATE_IDENTITY";
    public static final String UPDATE_NAME = "UPDATE_NAME";
    public static final String CHANGE_STATUS = "CHANGE_STATUS";
    public static final String CHANGE_STATUS_NO_OP = "CHANGE_STATUS_NO_OP";
    public static final String SET_RESTRICTED = "SET_RESTRICTED";
    public static final String VERIFY = "VERIFY";
    public static final String VERIFY_NOT_FOUND = "VERIFY_NOT_FOUND";

    private AuditActions() {
    }

    public static String rejected(String action) {
        return action + "_REJECTED";
    }
}

