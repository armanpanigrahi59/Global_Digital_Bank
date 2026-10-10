/** Thrown when a deposit or withdrawal amount is zero or negative. */
public class InvalidAmountException extends AccountException {
    public InvalidAmountException(double amount) {
        super("Invalid amount: " + amount + ". Amount must be greater than zero.");
    }
}
