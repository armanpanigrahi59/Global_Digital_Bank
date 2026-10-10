/** Thrown when a withdrawal exceeds the available balance. */
public class InsufficientBalanceException extends AccountException {
    public InsufficientBalanceException(double balance, double amount) {
        super("Insufficient balance: " + balance + " for withdrawal of " + amount);
    }
}
