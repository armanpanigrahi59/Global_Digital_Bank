/**
 * Fixed deposit: 6.5% annual interest, 5000 minimum opening deposit.
 * Withdrawals are only allowed once the deposit has matured.
 */
public class FixedDepositAccount extends BankAccount {
    private static final double MIN_BALANCE = 5000.0;
    private static final double INTEREST_RATE = 6.5;

    private final int tenureMonths;
    private boolean matured = false;

    public FixedDepositAccount(String accountNumber, String accountHolderName, int age,
                               double initialBalance, String pin) {
        this(accountNumber, accountHolderName, age, initialBalance, pin, 12);
    }

    public FixedDepositAccount(String accountNumber, String accountHolderName, int age,
                               double initialBalance, String pin, int tenureMonths) {
        super(accountNumber, accountHolderName, age, initialBalance, pin);
        this.tenureMonths = tenureMonths;
        validateOpeningBalance(initialBalance);
    }

    public int getTenureMonths() {
        return tenureMonths;
    }

    public boolean isMatured() {
        return matured;
    }

    /** Marks the deposit as matured (in a full system this would follow the calendar). */
    public void markMatured() {
        this.matured = true;
    }

    @Override
    public double getMinimumBalance() {
        return MIN_BALANCE;
    }

    @Override
    public String getAccountType() {
        return "FIXED_DEPOSIT";
    }

    @Override
    public double getInterestRate() {
        return INTEREST_RATE;
    }

    @Override
    public boolean canWithdraw(double amount) {
        return isActive() && matured && amount > 0 && amount <= balance;
    }
}
