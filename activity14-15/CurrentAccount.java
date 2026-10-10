/** Current: overdraft limit comes from the rules engine (feature "overdraftLimit"). */
public class CurrentAccount extends AbstractAccount {
    public CurrentAccount(String accountNumber, String accountHolderName, int age,
                          double initialBalance, String pin) {
        this(accountNumber, accountHolderName, age, initialBalance, pin, 0);
    }

    public CurrentAccount(String accountNumber, String accountHolderName, int age,
                          double initialBalance, String pin, int tenureYears) {
        super(accountNumber, accountHolderName, age, initialBalance, pin, tenureYears);
        validateOpeningBalance(initialBalance);
    }

    public double getOverdraftLimit() {
        return rules().getAdditionalFeatureAsDouble(getAccountType(), tenureYears, "overdraftLimit", 0.0);
    }

    @Override
    protected double getAvailableFunds() {
        return balance + getOverdraftLimit();
    }

    @Override
    public String getAccountType() {
        return "CURRENT";
    }

    @Override
    public boolean canWithdraw(double amount) {
        return isActive() && amount > 0 && amount <= getAvailableFunds();
    }
}
