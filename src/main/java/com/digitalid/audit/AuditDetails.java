package com.digitalid.audit;

public final class AuditDetails {
    private AuditDetails() {
    }

    public static String detail(String key, Object value) {
        return key + "=" + value;
    }

    public static String details(String... parts) {
        return String.join(",", parts);
    }
}

