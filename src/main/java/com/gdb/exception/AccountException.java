package com.gdb.exception;

/**
 * Base (checked) exception for every business-rule failure in the bank.
 * Callers can catch this one type to handle any account-related error.
 */
public class AccountException extends Exception {
    private static final long serialVersionUID = 1L;

    public AccountException(String message) {
        super(message);
    }

    public AccountException(String message, Throwable cause) {
        super(message, cause);
    }
}
