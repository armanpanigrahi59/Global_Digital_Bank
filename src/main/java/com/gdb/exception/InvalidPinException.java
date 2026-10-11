package com.gdb.exception;

/** An incorrect PIN was supplied. */
public class InvalidPinException extends AccountException {
    private static final long serialVersionUID = 1L;

    public InvalidPinException() {
        super("Invalid PIN provided");
    }
}
