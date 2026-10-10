/** Thrown when an account number is not in the registry. */
public class AccountNotFoundException extends AccountException {
    public AccountNotFoundException(String accountNumber) {
        super("Account not found: " + accountNumber);
    }
}
