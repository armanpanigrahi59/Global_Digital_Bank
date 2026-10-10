/**
 * Activity 7 - Savings account: earns interest, keeps the base withdrawal rules.
 */
public class SavingsAccount extends Account {
    private final double interestRate; // annual percentage, e.g. 5.0 = 5%

    public SavingsAccount(String accountNumber, String accountHolderName, int age,
                          double initialBalance, String pin, double interestRate) {
        super(accountNumber, accountHolderName, age, initialBalance, pin);
        this.interestRate = interestRate;
    }

    public double getInterestRate() {
        return interestRate;
    }

    /** Credits one period of interest to the balance (active accounts only). */
    public void applyInterest() {
        if (isActive()) {
            balance += balance * interestRate / 100;
        }
    }

    @Override
    public String getAccountType() {
        return "SAVINGS";
    }
}
