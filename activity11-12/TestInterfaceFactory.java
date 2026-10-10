/**
 * Activity 12 - Tests interface-based operations and factory-based creation.
 * Note that no concrete account class is referenced anywhere in this test.
 */
public class TestInterfaceFactory {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws AccountException {
        System.out.println("=== Factory creates every type behind IAccount ===");
        IAccount savings = AccountFactory.createAccount("SAVINGS", "S001", "Alice", 30, 1000.0, "1111");
        IAccount current = AccountFactory.createAccount("CURRENT", "C001", "Bob", 40, 2000.0, "2222");
        IAccount fixed = AccountFactory.createAccount("FIXED", "F001", "Carol", 50, 5000.0, "3333");
        IAccount salary = AccountFactory.createAccount("SALARY", "P001", "Dave", 28, 0.0, "4444");

        check("SAVINGS type", savings.getAccountType().equals("SAVINGS"));
        check("CURRENT type", current.getAccountType().equals("CURRENT"));
        check("FIXED maps to FIXED_DEPOSIT", fixed.getAccountType().equals("FIXED_DEPOSIT"));
        check("SALARY type", salary.getAccountType().equals("SALARY"));
        check("Account number kept", savings.getAccountNumber().equals("S001"));
        check("Holder name kept", current.getAccountHolderName().equals("Bob"));

        System.out.println("\n=== Operations through the interface ===");
        savings.deposit(500.0);
        System.out.println(savings.getAccountType() + ": " + savings.getBalance());
        check("Savings deposit -> 1500", savings.getBalance() == 1500.0);

        current.withdraw(300.0);
        System.out.println(current.getAccountType() + ": " + current.getBalance());
        check("Current withdraw -> 1700", current.getBalance() == 1700.0);

        salary.deposit(30000.0);
        salary.withdraw(30000.0);
        check("Salary account can be fully drawn down (0 minimum)", salary.getBalance() == 0.0);

        System.out.println("\n=== Runtime selection by type string ===");
        String[] types = {"savings", "Current", "FIXED_DEPOSIT", " SALARY "};
        double[] opening = {1000, 100, 6000, 10};
        String[] expected = {"SAVINGS", "CURRENT", "FIXED_DEPOSIT", "SALARY"};
        for (int i = 0; i < types.length; i++) {
            IAccount a = AccountFactory.createAccount(types[i], "R" + i, "User" + i, 30, opening[i], "0000");
            check("Runtime type '" + types[i] + "' -> " + a.getAccountType(), a.getAccountType().equals(expected[i]));
        }

        System.out.println("\n=== Polymorphic rules behind one interface ===");
        IAccount[] all = {savings, current, fixed, salary};
        double[] mins = {500, 0, 5000, 0};
        for (int i = 0; i < all.length; i++) {
            check(all[i].getAccountType() + " minimum balance " + mins[i], all[i].getMinimumBalance() == mins[i]);
        }
        check("Savings rate 4.0", savings.getInterestRate() == 4.0);
        check("Fixed rate 6.5", fixed.getInterestRate() == 6.5);

        try {
            savings.withdraw(1200); // 1500 - 1200 = 300 < 500
            check("Savings below minimum should throw", false);
        } catch (MinimumBalanceViolationException e) {
            check("Savings minimum enforced via interface", true);
        }
        try {
            fixed.withdraw(100);
            check("Fixed withdraw before maturity should throw", false);
        } catch (MinimumBalanceViolationException e) {
            check("Fixed deposit locked before maturity", true);
        }

        System.out.println("\n=== Lifecycle through the interface ===");
        savings.closeAccount("1111");
        check("Closed via interface", savings.getStatus() == AccountStatus.CLOSED && !savings.isActive());
        try {
            savings.deposit(10);
            check("Deposit on closed account should throw", false);
        } catch (InactiveAccountException e) {
            check("Closed account rejects deposit", true);
        }
        savings.reopenAccount("1111");
        check("Reopened via interface", savings.isActive());

        System.out.println("\n=== Factory error handling ===");
        expectFactoryError("Unknown type", () -> AccountFactory.createAccount("PLATINUM", "X", "X", 30, 1000, "1"));
        expectFactoryError("Null type", () -> AccountFactory.createAccount(null, "X", "X", 30, 1000, "1"));
        expectFactoryError("Underage", () -> AccountFactory.createAccount("SAVINGS", "X", "X", 17, 1000, "1"));
        expectFactoryError("Savings opening balance too low", () -> AccountFactory.createAccount("SAVINGS", "X", "X", 30, 100, "1"));

        System.out.println("\nResult: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void expectFactoryError(String label, Runnable r) {
        try {
            r.run();
            check(label + " should be rejected", false);
        } catch (IllegalArgumentException e) {
            check(label + " rejected: " + e.getMessage(), true);
        }
    }

    private static void check(String label, boolean ok) {
        if (ok) passed++; else failed++;
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + label);
    }
}
