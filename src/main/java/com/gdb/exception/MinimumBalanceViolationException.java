package com.gdb.exception;

/** An operation that would leave the account below its minimum balance. */
public class MinimumBalanceViolationException extends AccountException {
    private static final long serialVersionUID = 1L;

    public MinimumBalanceViolationException(double balance, double amount, double minBalance) {
        super("Minimum balance violation: withdrawing " + amount + " from " + balance
                + " would fall below the minimum of " + minBalance);
    }
}
