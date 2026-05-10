package com.digitalid.domain;

public enum DigitalIDStatus {
    ACTIVE {
        @Override
        public boolean canTransitionTo(DigitalIDStatus next) {
            return next == SUSPENDED || next == REVOKED;
        }
    },
    SUSPENDED {
        @Override
        public boolean canTransitionTo(DigitalIDStatus next) {
            return next == SUSPENDED || next == ACTIVE || next == REVOKED;
        }
    },
    REVOKED {
        @Override
        public boolean canTransitionTo(DigitalIDStatus next) {
            return next == REVOKED;
        }
    };
    public abstract boolean canTransitionTo(DigitalIDStatus next);
}