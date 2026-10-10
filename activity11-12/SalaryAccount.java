/**
 * Salary account: 3% annual interest, no minimum balance (zero-balance account),
 * with a monthly credit limit that describes the expected salary inflow.
 */
public class SalaryAccount extends AbstractAccount {
    private static final double MIN_BALANCE = 0.0;
    private static final double INTEREST_RATE = 3.0;
    private static final double DEFAULT_MONTHLY_CREDIT_LIMIT = 50000.0;

    private final double monthlyCreditLimit;

    public SalaryAccount(String accountNumber, String accountHolderName, int age,
                         double initialBalance, String pin) {
        this(accountNumber, accountHolderName, age, initialBalance, pin, DEFAULT_MONTHLY_CREDIT_LIMIT);
    }

    public SalaryAccount(String accountNumber, String accountHolderName, int age,
                         double initialBalance, String pin, double monthlyCreditLimit) {
        super(accountNumber, accountHolderName, age, initialBalance, pin);
        this.monthlyCreditLimit = monthlyCreditLimit;
        validateOpeningBalance(initialBalance);
    }

    public double getMonthlyCreditLimit() {
        return monthlyCreditLimit;
    }

    @Override
    public double getMinimumBalance() {
        return MIN_BALANCE;
    }

    @Override
    public String getAccountType() {
        return "SALARY";
    }

    @Override
    public double getInterestRate() {
        return INTEREST_RATE;
    }

    @Override
    public boolean canWithdraw(double amount) {
        return isActive() && amount > 0 && amount <= balance;
    }
}
