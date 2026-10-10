/**
 * Activity 5 - Account with exceptions.
 * Replaces the boolean error returns of Activity 3 with meaningful
 * checked exceptions. Builds on validation, PIN and status management.
 */
public class Account {
    public static final int MIN_AGE = 18;
    public static final double DEFAULT_MIN_BALANCE = 500.0;

    private final String accountNumber;
    private String accountHolderName;
    private final int age;
    private double balance;
    private final double minimumBalance;
    private String pin;
    private AccountStatus status;

    /**
     * @throws IllegalArgumentException for an under-age holder or too-low opening balance
     *                                  (constructor/programming errors, so left unchecked)
     */
    public Account(String accountNumber, String accountHolderName, int age,
                   double initialBalance, String pin) {
        if (age < MIN_AGE) {
            throw new IllegalArgumentException("Account holder must be at least " + MIN_AGE + " years old");
        }
        if (initialBalance < DEFAULT_MIN_BALANCE) {
            throw new IllegalArgumentException("Initial balance must be at least " + DEFAULT_MIN_BALANCE);
        }
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.age = age;
        this.balance = initialBalance;
        this.minimumBalance = DEFAULT_MIN_BALANCE;
        this.pin = pin;
        this.status = AccountStatus.ACTIVE;
    }

    // ---- PIN ----
    public void setPin(String pin) {
        this.pin = pin;
    }

    /** Succeeds silently; throws if the PIN is wrong. */
    public void verifyPin(String pin) throws InvalidPinException {
        if (this.pin == null || !this.pin.equals(pin)) {
            throw new InvalidPinException();
        }
    }

    // ---- Transactions ----
    public void deposit(double amount) throws InactiveAccountException, InvalidAmountException {
        requireActive();
        if (amount <= 0) {
            throw new InvalidAmountException(amount);
        }
        balance += amount;
    }

    public void withdraw(double amount) throws InactiveAccountException, InvalidAmountException,
            InsufficientBalanceException, MinimumBalanceViolationException {
        requireActive();
        if (amount <= 0) {
            throw new InvalidAmountException(amount);
        }
        if (amount > balance) {
            throw new InsufficientBalanceException(balance, amount);
        }
        if (balance - amount < minimumBalance) {
            throw new MinimumBalanceViolationException(balance, amount, minimumBalance);
        }
        balance -= amount;
    }

    // ---- Status management (PIN protected) ----
    public void closeAccount(String pin) throws InvalidPinException, InactiveAccountException {
        verifyPin(pin);
        if (status == AccountStatus.CLOSED) {
            throw new InactiveAccountException(accountNumber, status);
        }
        status = AccountStatus.CLOSED;
    }

    public void reopenAccount(String pin) throws InvalidPinException, AccountException {
        verifyPin(pin);
        if (status != AccountStatus.CLOSED) {
            throw new AccountException("Account " + accountNumber + " is not closed (status: " + status + ")");
        }
        status = AccountStatus.ACTIVE;
    }

    public void deactivateAccount(String pin) throws InvalidPinException, InactiveAccountException {
        verifyPin(pin);
        if (status != AccountStatus.ACTIVE) {
            throw new InactiveAccountException(accountNumber, status);
        }
        status = AccountStatus.INACTIVE;
    }

    private void requireActive() throws InactiveAccountException {
        if (status != AccountStatus.ACTIVE) {
            throw new InactiveAccountException(accountNumber, status);
        }
    }

    // ---- Getters ----
    public double getBalance() { return balance; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountHolderName() { return accountHolderName; }
    public int getAge() { return age; }
    public double getMinimumBalance() { return minimumBalance; }
    public AccountStatus getStatus() { return status; }
    public boolean isActive() { return status == AccountStatus.ACTIVE; }
}
