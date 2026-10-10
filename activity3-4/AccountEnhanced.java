/**
 * Activity 3 - Enhanced Account: age validation, minimum balance,
 * PIN protection and status management.
 * Failures are signalled with boolean returns (exceptions arrive in Activity 5).
 */
public class AccountEnhanced {
    public static final int MIN_AGE = 18;
    public static final double DEFAULT_MIN_BALANCE = 500.0;

    private final String accountNumber;
    private String accountHolderName;
    private final int age;
    private double balance;
    private final double minimumBalance;
    private String pin;
    private AccountStatus status;

    /**
     * @throws IllegalArgumentException if age is below 18, or the initial
     *                                  balance is below the minimum balance
     */
    public AccountEnhanced(String accountNumber, String accountHolderName, int age,
                           double initialBalance, String pin) {
        if (age < MIN_AGE) {
            throw new IllegalArgumentException("Account holder must be at least " + MIN_AGE + " years old");
        }
        if (initialBalance < DEFAULT_MIN_BALANCE) {
            throw new IllegalArgumentException("Initial balance must be at least " + DEFAULT_MIN_BALANCE);
        }
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.age = age;
        this.balance = initialBalance;
        this.minimumBalance = DEFAULT_MIN_BALANCE;
        this.pin = pin;
        this.status = AccountStatus.ACTIVE;
    }

    // ---- PIN ----
    public void setPin(String pin) {
        this.pin = pin;
    }

    public boolean verifyPin(String pin) {
        return this.pin != null && this.pin.equals(pin);
    }

    // ---- Transactions (only allowed when ACTIVE) ----
    public boolean deposit(double amount) {
        if (!isActive() || amount <= 0) {
            return false;
        }
        balance += amount;
        return true;
    }

    public boolean withdraw(double amount) {
        if (!isActive() || amount <= 0) {
            return false;
        }
        if (balance - amount < minimumBalance) {
            return false;
        }
        balance -= amount;
        return true;
    }

    // ---- Status management (PIN protected) ----
    public boolean closeAccount(String pin) {
        if (!verifyPin(pin) || status == AccountStatus.CLOSED) {
            return false;
        }
        status = AccountStatus.CLOSED;
        return true;
    }

    public boolean reopenAccount(String pin) {
        if (!verifyPin(pin) || status != AccountStatus.CLOSED) {
            return false;
        }
        status = AccountStatus.ACTIVE;
        return true;
    }

    public boolean deactivateAccount(String pin) {
        if (!verifyPin(pin) || status != AccountStatus.ACTIVE) {
            return false;
        }
        status = AccountStatus.INACTIVE;
        return true;
    }

    // ---- Getters ----
    public double getBalance() { return balance; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountHolderName() { return accountHolderName; }
    public int getAge() { return age; }
    public double getMinimumBalance() { return minimumBalance; }
    public AccountStatus getStatus() { return status; }
    public boolean isActive() { return status == AccountStatus.ACTIVE; }
}
