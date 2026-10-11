package com.gdb.exception;

/**
 * The transaction log could not be written or read. When thrown straight after
 * a money movement, the movement itself DID happen; only its log entry is missing.
 */
public class TransactionLogException extends AccountException {
    private static final long serialVersionUID = 1L;

    public TransactionLogException(String message, Throwable cause) {
        super(message + ": " + cause.getMessage(), cause);
    }
}
