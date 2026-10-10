/**
 * Activity 11 - The contract every account exposes to the rest of the system.
 * Client code (tests, services, UI) depends on this interface, never on a
 * concrete account class.
 */
public interface IAccount {
    String getAccountNumber();

    String getAccountHolderName();

    double getBalance();

    String getAccountType();

    AccountStatus getStatus();

    boolean isActive();

    double getMinimumBalance();

    double getInterestRate();

    boolean canWithdraw(double amount);

    void deposit(double amount) throws AccountException;

    void withdraw(double amount) throws AccountException;

    void verifyPin(String pin) throws InvalidPinException;

    void closeAccount(String pin) throws AccountException;

    void reopenAccount(String pin) throws AccountException;
}
