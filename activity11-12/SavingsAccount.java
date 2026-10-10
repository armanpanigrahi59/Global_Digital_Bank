/** Savings: 4% annual interest, 500 minimum balance. */
public class SavingsAccount extends AbstractAccount {
    private static final double MIN_BALANCE = 500.0;
    private static final double INTEREST_RATE = 4.0;

    public SavingsAccount(String accountNumber, String accountHolderName, int age,
                          double initialBalance, String pin) {
        super(accountNumber, accountHolderName, age, initialBalance, pin);
        validateOpeningBalance(initialBalance);
    }

    @Override
    public double getMinimumBalance() {
        return MIN_BALANCE;
    }

    @Override
    public String getAccountType() {
        return "SAVINGS";
    }

    @Override
    public double getInterestRate() {
        return INTEREST_RATE;
    }

    @Override
    public boolean canWithdraw(double amount) {
        return isActive() && amount > 0 && balance - amount >= getMinimumBalance();
    }
}
