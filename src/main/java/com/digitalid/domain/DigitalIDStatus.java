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
            return next == ACTIVE || next == REVOKED;
        }
    },
    REVOKED {
        @Override
        public boolean canTransitionTo(DigitalIDStatus next) {
            return false;
        }
    };
    public abstract boolean canTransitionTo(DigitalIDStatus next);
}