/**
 * Fixed deposit: withdrawals are only allowed once the deposit has matured.
 * The deposit term is taken as the account's tenure in years.
 */
public class FixedDepositAccount extends AbstractAccount {
    private boolean matured = false;

    public FixedDepositAccount(String accountNumber, String accountHolderName, int age,
                               double initialBalance, String pin) {
        this(accountNumber, accountHolderName, age, initialBalance, pin, 0);
    }

    public FixedDepositAccount(String accountNumber, String accountHolderName, int age,
                               double initialBalance, String pin, int tenureYears) {
        super(accountNumber, accountHolderName, age, initialBalance, pin, tenureYears);
        validateOpeningBalance(initialBalance);
    }

    public boolean isMatured() {
        return matured;
    }

    /** Marks the deposit as matured (a full system would follow the calendar). */
    public void markMatured() {
        this.matured = true;
    }

    @Override
    public String getAccountType() {
        return "FIXED_DEPOSIT";
    }

    @Override
    public boolean canWithdraw(double amount) {
        return isActive() && matured && amount > 0 && amount <= balance;
    }
}
