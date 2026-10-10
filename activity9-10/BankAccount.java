/**
 * Activity 9 - Abstract BankAccount.
 * Common state, PIN, status and exception handling live here; each concrete
 * account type supplies its own rules through the abstract methods.
 *
 * Template methods: withdraw() and applyMonthlyInterest() are fixed
 * algorithms that call the abstract hooks (getMinimumBalance, canWithdraw,
 * getInterestRate).
 */
public abstract class BankAccount {
    public static final int MIN_AGE = 18;

    protected final String accountNumber;
    protected String accountHolderName;
    protected final int age;
    protected double balance;
    protected String pin;
    protected AccountStatus status;

    /**
     * Validates the holder's age. The opening balance is validated by the
     * subclass constructor via {@link #validateOpeningBalance(double)}, because
     * the minimum balance is defined by the subclass.
     */
    protected BankAccount(String accountNumber, String accountHolderName, int age,
                          double initialBalance, String pin) {
        if (age < MIN_AGE) {
            throw new IllegalArgumentException("Account holder must be at least " + MIN_AGE + " years old");
        }
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.age = age;
        this.balance = initialBalance;
        this.pin = pin;
        this.status = AccountStatus.ACTIVE;
    }

    /** Called by subclass constructors once they are fully built. */
    protected final void validateOpeningBalance(double initialBalance) {
        if (initialBalance < getMinimumBalance()) {
            throw new IllegalArgumentException(getAccountType() + " accounts require an opening balance of at least "
                    + getMinimumBalance());
        }
    }

    // ---- Contract every account type must fulfil ----
    public abstract double getMinimumBalance();

    public abstract String getAccountType();

    /** Annual interest rate in percent. */
    public abstract double getInterestRate();

    /** True if the amount may be withdrawn right now under this type's rules. */
    public abstract boolean canWithdraw(double amount);

    // ---- Template methods ----
    /** Funds that may be drawn on; overridden where an overdraft exists. */
    protected double getAvailableFunds() {
        return balance;
    }

    public void withdraw(double amount) throws InactiveAccountException, InvalidAmountException,
            InsufficientBalanceException, MinimumBalanceViolationException {
        requireActive();
        if (amount <= 0) {
            throw new InvalidAmountException(amount);
        }
        if (amount > getAvailableFunds()) {
            throw new InsufficientBalanceException(balance, amount);
        }
        if (!canWithdraw(amount)) {
            throw new MinimumBalanceViolationException(balance, amount, getMinimumBalance());
        }
        balance -= amount;
    }

    /** Credits one month of interest (annual rate / 12) to active accounts. */
    public void applyMonthlyInterest() {
        if (isActive() && balance > 0) {
            balance += balance * getInterestRate() / 100 / 12;
        }
    }

    public void deposit(double amount) throws InactiveAccountException, InvalidAmountException {
        requireActive();
        if (amount <= 0) {
            throw new InvalidAmountException(amount);
        }
        balance += amount;
    }

    // ---- PIN ----
    public void setPin(String pin) {
        this.pin = pin;
    }

    public void verifyPin(String pin) throws InvalidPinException {
        if (this.pin == null || !this.pin.equals(pin)) {
            throw new InvalidPinException();
        }
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

    protected void requireActive() throws InactiveAccountException {
        if (status != AccountStatus.ACTIVE) {
            throw new InactiveAccountException(accountNumber, status);
        }
    }

    // ---- Getters ----
    public double getBalance() { return balance; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountHolderName() { return accountHolderName; }
    public int getAge() { return age; }
    public AccountStatus getStatus() { return status; }
    public boolean isActive() { return status == AccountStatus.ACTIVE; }
}
