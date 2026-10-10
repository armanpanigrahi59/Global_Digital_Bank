/** Current: no interest, no minimum balance, overdraft up to a fixed limit. */
public class CurrentAccount extends BankAccount {
    private static final double MIN_BALANCE = 0.0;
    private static final double INTEREST_RATE = 0.0;
    private static final double DEFAULT_OVERDRAFT = 1000.0;

    private final double overdraftLimit;

    public CurrentAccount(String accountNumber, String accountHolderName, int age,
                          double initialBalance, String pin) {
        this(accountNumber, accountHolderName, age, initialBalance, pin, DEFAULT_OVERDRAFT);
    }

    public CurrentAccount(String accountNumber, String accountHolderName, int age,
                          double initialBalance, String pin, double overdraftLimit) {
        super(accountNumber, accountHolderName, age, initialBalance, pin);
        this.overdraftLimit = overdraftLimit;
        validateOpeningBalance(initialBalance);
    }

    public double getOverdraftLimit() {
        return overdraftLimit;
    }

    @Override
    protected double getAvailableFunds() {
        return balance + overdraftLimit;
    }

    @Override
    public double getMinimumBalance() {
        return MIN_BALANCE;
    }

    @Override
    public String getAccountType() {
        return "CURRENT";
    }

    @Override
    public double getInterestRate() {
        return INTEREST_RATE;
    }

    @Override
    public boolean canWithdraw(double amount) {
        return isActive() && amount > 0 && amount <= getAvailableFunds();
    }
}
