/**
 * Shared account behaviour. From Activity 14 the type-specific numbers
 * (minimum balance, interest rate) are no longer hard-coded in subclasses:
 * they come from the AccountRulesEngine, keyed by account type and tenure.
 * Subclasses only supply what the rules table cannot express: their type
 * name and their withdrawal policy.
 */
public abstract class AbstractAccount implements IAccount {
    public static final int MIN_AGE = 18;

    protected final String accountNumber;
    protected String accountHolderName;
    protected final int age;
    protected final int tenureYears;
    protected double balance;
    protected String pin;
    protected AccountStatus status;

    protected AbstractAccount(String accountNumber, String accountHolderName, int age,
                              double initialBalance, String pin, int tenureYears) {
        if (age < MIN_AGE) {
            throw new IllegalArgumentException("Account holder must be at least " + MIN_AGE + " years old");
        }
        if (tenureYears < 0) {
            throw new IllegalArgumentException("Tenure must not be negative: " + tenureYears);
        }
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.age = age;
        this.tenureYears = tenureYears;
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

    protected AccountRulesEngine rules() {
        return AccountRulesEngine.getInstance();
    }

    // ---- Rule-driven values ----
    @Override
    public double getMinimumBalance() {
        return rules().getMinimumBalance(getAccountType(), tenureYears);
    }

    @Override
    public double getInterestRate() {
        return rules().getInterestRate(getAccountType(), tenureYears);
    }

    public String getFeatureName() {
        return rules().getFeatureName(getAccountType(), tenureYears);
    }

    // ---- Contract for subclasses ----
    @Override
    public abstract String getAccountType();

    @Override
    public abstract boolean canWithdraw(double amount);

    /** Funds that may be drawn on; overridden where an overdraft exists. */
    protected double getAvailableFunds() {
        return balance;
    }

    // ---- Transactions ----
    @Override
    public void deposit(double amount) throws InactiveAccountException, InvalidAmountException {
        requireActive();
        if (amount <= 0) {
            throw new InvalidAmountException(amount);
        }
        balance += amount;
    }

    @Override
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

    /** Credits one month of interest (annual rate / 12) to active accounts. */
    public void applyMonthlyInterest() {
        if (isActive() && balance > 0) {
            balance += balance * getInterestRate() / 100 / 12;
        }
    }

    // ---- PIN ----
    public void setPin(String pin) {
        this.pin = pin;
    }

    @Override
    public void verifyPin(String pin) throws InvalidPinException {
        if (this.pin == null || !this.pin.equals(pin)) {
            throw new InvalidPinException();
        }
    }

    // ---- Status management (PIN protected) ----
    @Override
    public void closeAccount(String pin) throws InvalidPinException, InactiveAccountException {
        verifyPin(pin);
        if (status == AccountStatus.CLOSED) {
            throw new InactiveAccountException(accountNumber, status);
        }
        status = AccountStatus.CLOSED;
    }

    @Override
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
    @Override public double getBalance() { return balance; }
    @Override public String getAccountNumber() { return accountNumber; }
    @Override public String getAccountHolderName() { return accountHolderName; }
    @Override public int getTenureYears() { return tenureYears; }
    @Override public AccountStatus getStatus() { return status; }
    @Override public boolean isActive() { return status == AccountStatus.ACTIVE; }
    public int getAge() { return age; }
}
