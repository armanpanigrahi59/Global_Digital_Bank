package com.gdb.exception;

/** A withdrawal blocked by a product rule other than balance, e.g. a fixed deposit that has not matured. */
public class WithdrawalNotAllowedException extends AccountException {
    private static final long serialVersionUID = 1L;

    public WithdrawalNotAllowedException(String reason) {
        super("Withdrawal not allowed: " + reason);
    }
}
