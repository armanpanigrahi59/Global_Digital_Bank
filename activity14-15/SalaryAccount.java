/** Salary: zero-balance account; the monthly credit limit comes from the rules engine. */
public class SalaryAccount extends AbstractAccount {
    public SalaryAccount(String accountNumber, String accountHolderName, int age,
                         double initialBalance, String pin) {
        this(accountNumber, accountHolderName, age, initialBalance, pin, 0);
    }

    public SalaryAccount(String accountNumber, String accountHolderName, int age,
                         double initialBalance, String pin, int tenureYears) {
        super(accountNumber, accountHolderName, age, initialBalance, pin, tenureYears);
        validateOpeningBalance(initialBalance);
    }

    public double getMonthlyCreditLimit() {
        return rules().getAdditionalFeatureAsDouble(getAccountType(), tenureYears, "monthlyCreditLimit", 0.0);
    }

    @Override
    public String getAccountType() {
        return "SALARY";
    }

    @Override
    public boolean canWithdraw(double amount) {
        return isActive() && amount > 0 && amount <= balance - getMinimumBalance();
    }
}
