/** Thrown when an incorrect PIN is provided. */
public class InvalidPinException extends AccountException {
    public InvalidPinException() {
        super("Invalid PIN provided");
    }
}
