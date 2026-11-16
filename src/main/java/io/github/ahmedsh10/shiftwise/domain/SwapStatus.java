package io.github.ahmedsh10.shiftwise.domain;

public enum SwapStatus {
    /** Waiting for the colleague to accept. */
    PENDING_COLLEAGUE,
    /** Colleague accepted; waiting for a supervisor. */
    PENDING_SUPERVISOR,
    APPROVED,
    REJECTED,
    CANCELLED;

    public boolean isOpen() {
        return this == PENDING_COLLEAGUE || this == PENDING_SUPERVISOR;
    }
}
