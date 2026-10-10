/**
 * The contract every account exposes to the rest of the system.
 * Activity 16 adds the *WithTransaction methods. They coexist with the void
 * deposit()/withdraw() so earlier code keeps working unchanged.
 */
public interface IAccount {
    String getAccountNumber();

    String getAccountHolderName();

    double getBalance();

    String getAccountType();

    /** Whole years the customer has held this account; selects the rules bucket. */
    int getTenureYears();

    AccountStatus getStatus();

    boolean isActive();

    double getMinimumBalance();

    double getInterestRate();

    boolean canWithdraw(double amount);

    void deposit(double amount) throws AccountException;

    void withdraw(double amount) throws AccountException;

    /** Same as deposit(), but returns the record of what happened. */
    Transaction depositWithTransaction(double amount) throws AccountException;

    /** Same as withdraw(), but returns the record of what happened. */
    Transaction withdrawWithTransaction(double amount) throws AccountException;

    void verifyPin(String pin) throws InvalidPinException;

    void closeAccount(String pin) throws AccountException;

    void reopenAccount(String pin) throws AccountException;
}
