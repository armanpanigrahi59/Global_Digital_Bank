/**
 * Creates accounts by type name and returns them as IAccount.
 * Activity 14 adds an overload that takes the customer's tenure in years.
 */
public final class AccountFactory {
    private AccountFactory() {
    }

    public static IAccount createAccount(String type, String accountNumber, String accountHolderName,
                                         int age, double initialBalance, String pin) {
        return createAccount(type, accountNumber, accountHolderName, age, initialBalance, pin, 0);
    }

    /**
     * @param type SAVINGS, CURRENT, FIXED (or FIXED_DEPOSIT) or SALARY; case-insensitive
     * @throws IllegalArgumentException for an unknown type or invalid opening details
     */
    public static IAccount createAccount(String type, String accountNumber, String accountHolderName,
                                         int age, double initialBalance, String pin, int tenureYears) {
        if (type == null) {
            throw new IllegalArgumentException("Account type must not be null");
        }
        return switch (type.trim().toUpperCase()) {
            case "SAVINGS" -> new SavingsAccount(accountNumber, accountHolderName, age, initialBalance, pin, tenureYears);
            case "CURRENT" -> new CurrentAccount(accountNumber, accountHolderName, age, initialBalance, pin, tenureYears);
            case "FIXED", "FIXED_DEPOSIT" -> new FixedDepositAccount(accountNumber, accountHolderName, age, initialBalance, pin, tenureYears);
            case "SALARY" -> new SalaryAccount(accountNumber, accountHolderName, age, initialBalance, pin, tenureYears);
            default -> throw new IllegalArgumentException("Unknown account type: " + type);
        };
    }
}
