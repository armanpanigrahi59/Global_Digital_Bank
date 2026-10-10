import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Activity 15 - Moves money between two accounts, enforcing a daily transfer
 * limit per source account.
 *
 * The limit comes from the rules engine (type + tenure of the source account,
 * so longer-standing customers get a higher "privilege" limit). The running
 * total is kept per account and resets automatically when the date changes.
 *
 * A transfer is all-or-nothing: if crediting the target fails after the
 * source was debited, the debit is reversed.
 */
public class TransferService {
    /** Running total for one source account on one day. */
    private static final class DailyTotal {
        double total = 0;
        LocalDate date;
    }

    private final Map<String, DailyTotal> totals = new HashMap<>();
    private final Supplier<LocalDate> today;

    public TransferService() {
        this(LocalDate::now);
    }

    /** The date source is injectable so tests can simulate a new day. */
    public TransferService(Supplier<LocalDate> today) {
        this.today = today;
    }

    public double getDailyTransferLimit(IAccount account) {
        return AccountRulesEngine.getInstance()
                .getDailyTransferLimit(account.getAccountType(), account.getTenureYears());
    }

    public synchronized double getDailyTransferTotal(IAccount account) {
        return totalFor(account).total;
    }

    public synchronized double getRemainingDailyLimit(IAccount account) {
        return Math.max(0, getDailyTransferLimit(account) - totalFor(account).total);
    }

    public synchronized boolean canTransfer(IAccount from, double amount) {
        return amount > 0 && totalFor(from).total + amount <= getDailyTransferLimit(from);
    }

    /**
     * @return the record of the completed transfer (type TRANSFER, on the source account)
     * @throws InvalidPinException        wrong PIN for the source account
     * @throws InvalidAmountException     amount is zero or negative
     * @throws InactiveAccountException   source or target is not active
     * @throws DailyLimitExceededException the daily limit would be exceeded
     * @throws AccountException           also for same-account transfers; the withdrawal
     *                                    exceptions (insufficient funds, minimum balance) pass through
     */
    public synchronized Transaction transfer(IAccount from, IAccount to, double amount, String pin)
            throws AccountException {
        if (from.getAccountNumber().equals(to.getAccountNumber())) {
            throw new AccountException("Cannot transfer to the same account: " + from.getAccountNumber());
        }
        from.verifyPin(pin);
        if (amount <= 0) {
            throw new InvalidAmountException(amount);
        }
        if (!from.isActive()) {
            throw new InactiveAccountException(from.getAccountNumber(), from.getStatus());
        }
        if (!to.isActive()) {
            throw new InactiveAccountException(to.getAccountNumber(), to.getStatus());
        }
        DailyTotal daily = totalFor(from);
        double limit = getDailyTransferLimit(from);
        if (daily.total + amount > limit) {
            throw new DailyLimitExceededException(amount, daily.total, limit);
        }

        from.withdraw(amount); // may throw: nothing has changed yet
        try {
            to.deposit(amount);
        } catch (AccountException e) {
            try {
                from.deposit(amount); // undo the debit
            } catch (AccountException rollbackFailure) {
                throw new IllegalStateException("Transfer failed and could not be reversed for account "
                        + from.getAccountNumber(), rollbackFailure);
            }
            throw e;
        }
        daily.total += amount;
        return new Transaction(TransactionType.TRANSFER, amount, from.getAccountNumber(),
                to.getAccountNumber(), from.getBalance(),
                "Transfer from " + from.getAccountNumber() + " to " + to.getAccountNumber());
    }

    /** Returns the account's total for today, resetting it first if the day has changed. */
    private DailyTotal totalFor(IAccount account) {
        DailyTotal t = totals.computeIfAbsent(account.getAccountNumber(), k -> new DailyTotal());
        LocalDate now = today.get();
        if (!now.equals(t.date)) {
            t.total = 0;
            t.date = now;
        }
        return t;
    }
}
