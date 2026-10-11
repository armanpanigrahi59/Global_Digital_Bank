package com.gdb.exception;

/** An operation on an account that is not ACTIVE (inactive or closed). */
public class InactiveAccountException extends AccountException {
    private static final long serialVersionUID = 1L;

    /**
     * @param status the account's current status as text (kept a String so this
     *               package has no dependency on the domain package)
     */
    public InactiveAccountException(String accountNumber, String status) {
        super("Account " + accountNumber + " is not active (status: " + status + ")");
    }
}
