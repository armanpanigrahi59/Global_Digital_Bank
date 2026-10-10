/** Thrown when an operation is attempted on an inactive or closed account. */
public class InactiveAccountException extends AccountException {
    public InactiveAccountException(String accountNumber, AccountStatus status) {
        super("Account " + accountNumber + " is not active (status: " + status + ")");
    }
}
