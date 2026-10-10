/**
 * Activity 7 - Current account: allows withdrawals beyond the balance
 * up to an overdraft limit (the minimum-balance rule does not apply).
 */
public class CurrentAccount extends Account {
    private final double overdraftLimit;

    public CurrentAccount(String accountNumber, String accountHolderName, int age,
                          double initialBalance, String pin, double overdraftLimit) {
        super(accountNumber, accountHolderName, age, initialBalance, pin);
        this.overdraftLimit = overdraftLimit;
    }

    public double getOverdraftLimit() {
        return overdraftLimit;
    }

    @Override
    public boolean canWithdraw(double amount) {
        return isActive() && amount > 0 && amount <= balance + overdraftLimit;
    }

    @Override
    public void withdraw(double amount) throws InactiveAccountException, InvalidAmountException,
            InsufficientBalanceException, MinimumBalanceViolationException {
        requireActive();
        if (amount <= 0) {
            throw new InvalidAmountException(amount);
        }
        if (amount > balance + overdraftLimit) {
            throw new InsufficientBalanceException(balance, amount);
        }
        balance -= amount;
    }

    @Override
    public String getAccountType() {
        return "CURRENT";
    }
}
