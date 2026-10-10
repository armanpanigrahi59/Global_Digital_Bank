/** Savings: rate and minimum balance come from the rules engine. */
public class SavingsAccount extends AbstractAccount {
    public SavingsAccount(String accountNumber, String accountHolderName, int age,
                          double initialBalance, String pin) {
        this(accountNumber, accountHolderName, age, initialBalance, pin, 0);
    }

    public SavingsAccount(String accountNumber, String accountHolderName, int age,
                          double initialBalance, String pin, int tenureYears) {
        super(accountNumber, accountHolderName, age, initialBalance, pin, tenureYears);
        validateOpeningBalance(initialBalance);
    }

    @Override
    public String getAccountType() {
        return "SAVINGS";
    }

    @Override
    public boolean canWithdraw(double amount) {
        return isActive() && amount > 0 && balance - amount >= getMinimumBalance();
    }
}
