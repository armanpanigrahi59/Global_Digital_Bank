package com.gdb.exception;

/** A deposit, withdrawal or transfer amount that is zero, negative or not a finite number. */
public class InvalidAmountException extends AccountException {
    private static final long serialVersionUID = 1L;

    public InvalidAmountException(double amount) {
        super("Invalid amount: " + amount + ". Amount must be greater than zero.");
    }
}
