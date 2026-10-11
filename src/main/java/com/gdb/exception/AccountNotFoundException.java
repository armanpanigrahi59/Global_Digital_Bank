package com.gdb.exception;

/** An account number that is not in the registry. */
public class AccountNotFoundException extends AccountException {
    private static final long serialVersionUID = 1L;

    public AccountNotFoundException(String accountNumber) {
        super("Account not found: " + accountNumber);
    }
}
