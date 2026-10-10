/** Thrown when a transfer would push an account over its daily transfer limit. */
public class DailyLimitExceededException extends AccountException {
    public DailyLimitExceededException(double amount, double alreadyTransferred, double limit) {
        super("Daily transfer limit exceeded: transferring " + amount + " on top of " + alreadyTransferred
                + " already sent today would exceed the limit of " + limit);
    }
}
