/**
 * Activity 10 - Tests abstraction, polymorphism and type-specific rules
 * across every BankAccount subtype through a single BankAccount[] array.
 */
public class TestAbstractAccount {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws AccountException {
        BankAccount[] accounts = {
                new SavingsAccount("S001", "Alice", 30, 1000.0, "1111"),
                new CurrentAccount("C001", "Bob", 40, 2000.0, "2222"),
                new FixedDepositAccount("F001", "Carol", 50, 5000.0, "3333")
        };

        System.out.println("=== Abstract dispatch over BankAccount[] ===");
        for (BankAccount acc : accounts) {
            System.out.println(acc.getAccountType()
                    + " | Min Balance: " + acc.getMinimumBalance()
                    + " | Rate: " + acc.getInterestRate());
            double before = acc.getBalance();
            acc.applyMonthlyInterest();
            System.out.println("  Balance " + before + " -> " + acc.getBalance());
        }
        check("Savings earns monthly interest (1000 -> 1003.33)", near(accounts[0].getBalance(), 1000 + 1000 * 4.0 / 100 / 12));
        check("Current earns no interest (2000 stays)", accounts[1].getBalance() == 2000.0);
        check("Fixed deposit earns monthly interest", near(accounts[2].getBalance(), 5000 + 5000 * 6.5 / 100 / 12));

        System.out.println("\n=== Type-specific minimum balances and rates ===");
        check("Savings min 500", accounts[0].getMinimumBalance() == 500.0);
        check("Current min 0", accounts[1].getMinimumBalance() == 0.0);
        check("Fixed min 5000", accounts[2].getMinimumBalance() == 5000.0);
        check("Types are distinct", !accounts[0].getAccountType().equals(accounts[1].getAccountType())
                && !accounts[1].getAccountType().equals(accounts[2].getAccountType()));

        System.out.println("\n=== Savings withdraw rules ===");
        BankAccount savings = accounts[0];
        check("canWithdraw within limit", savings.canWithdraw(400));
        check("canWithdraw below minimum blocked", !savings.canWithdraw(600));
        expectMinViolation("Savings withdraw below minimum", savings, 600);
        savings.withdraw(400);
        check("Savings withdraw within limit works", near(savings.getBalance(), 1000 + 1000 * 4.0 / 100 / 12 - 400));
        expectInsufficient("Savings withdraw more than balance", savings, 100000);

        System.out.println("\n=== Current withdraw rules (overdraft) ===");
        BankAccount current = accounts[1];
        check("canWithdraw into overdraft (2500 <= 2000 + 1000)", current.canWithdraw(2500));
        current.withdraw(2500);
        check("Balance is -500", current.getBalance() == -500.0);
        expectInsufficient("Current withdraw beyond overdraft", current, 600);

        System.out.println("\n=== Fixed deposit withdraw rules ===");
        FixedDepositAccount fixed = (FixedDepositAccount) accounts[2];
        check("Not matured initially", !fixed.isMatured());
        check("canWithdraw false before maturity", !fixed.canWithdraw(100));
        expectMinViolation("Fixed withdraw before maturity", fixed, 100);
        fixed.markMatured();
        check("canWithdraw true after maturity", fixed.canWithdraw(100));
        fixed.withdraw(100);
        check("Fixed withdraw after maturity works", near(fixed.getBalance(), 5000 + 5000 * 6.5 / 100 / 12 - 100));

        System.out.println("\n=== Common behaviour inherited from BankAccount ===");
        BankAccount any = accounts[0];
        try {
            any.verifyPin("0000");
            check("Wrong PIN should throw", false);
        } catch (InvalidPinException e) {
            check("Wrong PIN rejected via base type", true);
        }
        any.closeAccount("1111");
        try {
            any.deposit(10);
            check("Deposit on closed account should throw", false);
        } catch (InactiveAccountException e) {
            check("Closed account blocks deposit", true);
        }
        double closedBalance = any.getBalance();
        any.applyMonthlyInterest();
        check("No interest on closed account", any.getBalance() == closedBalance);

        System.out.println("\n=== Constructor validation ===");
        expectIllegalArgument("Savings opening balance below 500", () -> new SavingsAccount("S9", "X", 30, 100.0, "1"));
        expectIllegalArgument("Fixed opening balance below 5000", () -> new FixedDepositAccount("F9", "X", 30, 1000.0, "1"));
        expectIllegalArgument("Underage holder", () -> new CurrentAccount("C9", "X", 16, 100.0, "1"));

        System.out.println("\nResult: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void expectMinViolation(String label, BankAccount acc, double amount) throws AccountException {
        try {
            acc.withdraw(amount);
            check(label + " should throw MinimumBalanceViolationException", false);
        } catch (MinimumBalanceViolationException e) {
            check(label + " -> MinimumBalanceViolationException", true);
        }
    }

    private static void expectInsufficient(String label, BankAccount acc, double amount) throws AccountException {
        try {
            acc.withdraw(amount);
            check(label + " should throw InsufficientBalanceException", false);
        } catch (InsufficientBalanceException e) {
            check(label + " -> InsufficientBalanceException", true);
        }
    }

    private static void expectIllegalArgument(String label, Runnable r) {
        try {
            r.run();
            check(label + " should be rejected", false);
        } catch (IllegalArgumentException e) {
            check(label + " rejected: " + e.getMessage(), true);
        }
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1e-9;
    }

    private static void check(String label, boolean ok) {
        if (ok) passed++; else failed++;
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + label);
    }
}
