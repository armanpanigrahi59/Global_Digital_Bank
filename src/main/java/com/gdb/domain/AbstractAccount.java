package com.gdb.domain;

import com.gdb.exception.AccountException;
import com.gdb.exception.InactiveAccountException;
import com.gdb.exception.InsufficientBalanceException;
import com.gdb.exception.InvalidAmountException;
import com.gdb.exception.InvalidPinException;
import com.gdb.exception.MinimumBalanceViolationException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;

/**
 * Behaviour shared by every account: identity, PIN, status lifecycle,
 * deposits, withdrawals and interest.
 *
 * Product differences are limited to two hooks: {@link #availableFunds()}
 * (what can be drawn on) and {@link #checkWithdrawalRules(double)} (extra
 * product rules). Numbers such as minimum balance and interest rate are never
 * hard-coded; they come from the injected {@link AccountRules}.
 *
 * Not thread-safe: callers (AccountService) serialize access.
 */
public abstract class AbstractAccount implements IAccount {
    public static final int MIN_AGE = 18;
    private static final Pattern PIN_FORMAT = Pattern.compile("\\d{4}");

    private final AccountType type;
    private final AccountRules rules;
    private final String accountNumber;
    private final String accountHolderName;
    private final int tenureYears;
    private final String pin;
    private double balance;
    private AccountStatus status = AccountStatus.ACTIVE;

    /**
     * @throws IllegalArgumentException if any detail is invalid (missing type or rules, blank
     *         name or number, age under 18, negative tenure, PIN not exactly 4 digits, opening
     *         balance negative or below the product's minimum)
     */
    protected AbstractAccount(AccountType type, AccountRules rules, String accountNumber,
                              String accountHolderName, int age, double initialBalance, String pin,
                              int tenureYears) {
        if (type == null) {
            throw new IllegalArgumentException("Account type is required");
        }
        if (rules == null) {
            throw new IllegalArgumentException("Account rules are required");
        }
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number is required");
        }
        if (accountHolderName == null || accountHolderName.isBlank()) {
            throw new IllegalArgumentException("Account holder name is required");
        }
        if (age < MIN_AGE) {
            throw new IllegalArgumentException("Account holder must be at least " + MIN_AGE + " years old");
        }
        if (tenureYears < 0) {
            throw new IllegalArgumentException("Tenure must not be negative: " + tenureYears);
        }
        if (pin == null || !PIN_FORMAT.matcher(pin).matches()) {
            throw new IllegalArgumentException("PIN must be exactly 4 digits");
        }
        if (Double.isNaN(initialBalance) || Double.isInfinite(initialBalance) || initialBalance < 0) {
            throw new IllegalArgumentException("Opening balance must be a non-negative number");
        }
        double minimum = rules.getMinimumBalance(type, tenureYears);
        if (initialBalance < minimum) {
            throw new IllegalArgumentException(type.getDisplayName()
                    + " accounts require an opening balance of at least " + minimum);
        }
        this.type = type;
        this.rules = rules;
        this.accountNumber = accountNumber.trim();
        this.accountHolderName = accountHolderName.trim();
        this.tenureYears = tenureYears;
        this.pin = pin;
        this.balance = initialBalance;
    }

    // ---- Product hooks ----

    /** Funds that may be drawn on. Overridden where an overdraft exists. */
    protected double availableFunds() {
        return balance;
    }

    /**
     * Extra product rules for a withdrawal that is positive and within
     * {@link #availableFunds()}. The default enforces the minimum balance.
     */
    protected void checkWithdrawalRules(double amount) throws AccountException {
        double minimum = getMinimumBalance();
        if (balance - amount < minimum) {
            throw new MinimumBalanceViolationException(balance, amount, minimum);
        }
    }

    protected AccountRules rules() {
        return rules;
    }

    // ---- Transactions ----

    @Override
    public void deposit(double amount) throws AccountException {
        requireActive();
        requirePositive(amount);
        balance += amount;
    }

    @Override
    public void withdraw(double amount) throws AccountException {
        validateWithdrawal(amount);
        balance -= amount;
    }

    @Override
    public boolean canWithdraw(double amount) {
        try {
            validateWithdrawal(amount);
            return true;
        } catch (AccountException e) {
            return false;
        }
    }

    @Override
    public Transaction depositWithTransaction(double amount) throws AccountException {
        deposit(amount);
        return new Transaction(TransactionType.DEPOSIT, amount, accountNumber, null, balance,
                "Deposit to " + accountNumber);
    }

    @Override
    public Transaction withdrawWithTransaction(double amount) throws AccountException {
        withdraw(amount);
        return new Transaction(TransactionType.WITHDRAW, amount, accountNumber, null, balance,
                "Withdrawal from " + accountNumber);
    }

    @Override
    public void applyMonthlyInterest() {
        if (isActive() && balance > 0) {
            balance += balance * getInterestRate() / 100 / 12;
        }
    }

    private void validateWithdrawal(double amount) throws AccountException {
        requireActive();
        requirePositive(amount);
        if (amount > availableFunds()) {
            throw new InsufficientBalanceException(balance, amount);
        }
        checkWithdrawalRules(amount);
    }

    private void requirePositive(double amount) throws InvalidAmountException {
        if (!(amount > 0) || Double.isInfinite(amount)) { // also rejects NaN
            throw new InvalidAmountException(amount);
        }
    }

    private void requireActive() throws InactiveAccountException {
        if (status != AccountStatus.ACTIVE) {
            throw new InactiveAccountException(accountNumber, status.name());
        }
    }

    // ---- PIN ----

    @Override
    public void verifyPin(String candidate) throws InvalidPinException {
        if (candidate == null || !MessageDigest.isEqual(
                pin.getBytes(StandardCharsets.UTF_8), candidate.getBytes(StandardCharsets.UTF_8))) {
            throw new InvalidPinException();
        }
    }

    // ---- Status lifecycle (all PIN protected) ----

    @Override
    public void closeAccount(String candidatePin) throws AccountException {
        verifyPin(candidatePin);
        if (status == AccountStatus.CLOSED) {
            throw new InactiveAccountException(accountNumber, status.name());
        }
        status = AccountStatus.CLOSED;
    }

    @Override
    public void reopenAccount(String candidatePin) throws AccountException {
        verifyPin(candidatePin);
        if (status == AccountStatus.ACTIVE) {
            throw new AccountException("Account " + accountNumber + " is already active");
        }
        status = AccountStatus.ACTIVE;
    }

    @Override
    public void deactivateAccount(String candidatePin) throws AccountException {
        verifyPin(candidatePin);
        requireActive();
        status = AccountStatus.INACTIVE;
    }

    // ---- Rule-driven values ----

    @Override
    public double getMinimumBalance() {
        return rules.getMinimumBalance(type, tenureYears);
    }

    @Override
    public double getInterestRate() {
        return rules.getInterestRate(type, tenureYears);
    }

    /** Marketing name of this account's product tier, e.g. "Premium Savings". */
    public String getFeatureName() {
        return rules.getFeatureName(type, tenureYears);
    }

    // ---- Getters ----

    @Override
    public double getBalance() {
        return balance;
    }

    @Override
    public AccountType getAccountType() {
        return type;
    }

    @Override
    public String getAccountNumber() {
        return accountNumber;
    }

    @Override
    public String getAccountHolderName() {
        return accountHolderName;
    }

    @Override
    public int getTenureYears() {
        return tenureYears;
    }

    @Override
    public AccountStatus getStatus() {
        return status;
    }

    @Override
    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    @Override
    public String toString() {
        return getAccountType().getDisplayName() + " account " + accountNumber + " [" + status + "]";
    }
}
