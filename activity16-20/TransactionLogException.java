/**
 * Thrown when the log could not be written or read. If it is thrown after a
 * money movement, the movement itself DID happen; only its log entry is missing.
 */
public class TransactionLogException extends AccountException {
    public TransactionLogException(String message, Throwable cause) {
        super(message + ": " + cause.getMessage());
        initCause(cause);
    }
}
