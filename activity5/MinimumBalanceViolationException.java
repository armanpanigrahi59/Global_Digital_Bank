/** Thrown when an operation would drop the balance below the minimum. */
public class MinimumBalanceViolationException extends AccountException {
    public MinimumBalanceViolationException(double balance, double amount, double minBalance) {
        super("Minimum balance violation: withdrawing " + amount + " from " + balance
                + " would fall below the minimum of " + minBalance);
    }
}
