package com.gdb.exception;

/** A withdrawal larger than the funds available to the account. */
public class InsufficientBalanceException extends AccountException {
    private static final long serialVersionUID = 1L;

    public InsufficientBalanceException(double balance, double amount) {
        super("Insufficient balance: " + balance + " for withdrawal of " + amount);
    }
}
