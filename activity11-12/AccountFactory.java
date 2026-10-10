/**
 * Activity 11 - Factory Pattern.
 * Callers ask for an account by type name and get back an IAccount, so they
 * never depend on (or even know about) the concrete classes.
 */
public final class AccountFactory {
    private AccountFactory() {
    }

    /**
     * @param type SAVINGS, CURRENT, FIXED (or FIXED_DEPOSIT) or SALARY; case-insensitive
     * @throws IllegalArgumentException for an unknown type or invalid opening details
     */
    public static IAccount createAccount(String type, String accountNumber, String accountHolderName,
                                         int age, double initialBalance, String pin) {
        if (type == null) {
            throw new IllegalArgumentException("Account type must not be null");
        }
        return switch (type.trim().toUpperCase()) {
            case "SAVINGS" -> new SavingsAccount(accountNumber, accountHolderName, age, initialBalance, pin);
            case "CURRENT" -> new CurrentAccount(accountNumber, accountHolderName, age, initialBalance, pin);
            case "FIXED", "FIXED_DEPOSIT" -> new FixedDepositAccount(accountNumber, accountHolderName, age, initialBalance, pin);
            case "SALARY" -> new SalaryAccount(accountNumber, accountHolderName, age, initialBalance, pin);
            default -> throw new IllegalArgumentException("Unknown account type: " + type);
        };
    }
}
