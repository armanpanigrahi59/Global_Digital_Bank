package com.gdb.domain;

import com.gdb.exception.AccountException;
import com.gdb.exception.DailyLimitExceededException;
import com.gdb.exception.InactiveAccountException;
import com.gdb.exception.InvalidAmountException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Domain service that moves money between two accounts while enforcing a
 * daily transfer limit per source account.
 *
 * The limit comes from the rules (product and tenure of the source account,
 * so longer-standing customers get a higher limit). The running total is kept
 * per account and resets when the date changes. A transfer is all-or-nothing:
 * if crediting the target fails after the source was debited, the debit is reversed.
 */
public class TransferService {
    /** Running total for one source account on one day. */
    private static final class DailyTotal {
        double total = 0;
        LocalDate date;
    }

    private final AccountRules rules;
    private final Supplier<LocalDate> today;
    private final Map<String, DailyTotal> totals = new HashMap<>();

    public TransferService(AccountRules rules) {
        this(rules, LocalDate::now);
    }

    /** The date source is injectable so tests can simulate a new day. */
    public TransferService(AccountRules rules, Supplier<LocalDate> today) {
        if (rules == null || today == null) {
            throw new IllegalArgumentException("Rules and date source are required");
        }
        this.rules = rules;
        this.today = today;
    }

    public double getDailyTransferLimit(IAccount account) {
        return rules.getDailyTransferLimit(account.getAccountType(), account.getTenureYears());
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
     * @throws com.gdb.exception.InvalidPinException wrong PIN for the source account
     * @throws InvalidAmountException                 amount is not a positive number
     * @throws InactiveAccountException               source or target is not active
     * @throws DailyLimitExceededException            the daily limit would be exceeded
     * @throws AccountException                       also for same-account transfers; withdrawal
     *                                                failures (insufficient funds, minimum balance,
     *                                                locked deposit) pass through unchanged
     */
    public synchronized Transaction transfer(IAccount from, IAccount to, double amount, String pin)
            throws AccountException {
        if (from.getAccountNumber().equals(to.getAccountNumber())) {
            throw new AccountException("Cannot transfer to the same account: " + from.getAccountNumber());
        }
        from.verifyPin(pin);
        if (!(amount > 0) || Double.isInfinite(amount)) {
            throw new InvalidAmountException(amount);
        }
        if (!from.isActive()) {
            throw new InactiveAccountException(from.getAccountNumber(), from.getStatus().name());
        }
        if (!to.isActive()) {
            throw new InactiveAccountException(to.getAccountNumber(), to.getStatus().name());
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
