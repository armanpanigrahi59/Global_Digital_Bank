/**
 * Activity 8 - Tests type-specific behaviour (interest, overdraft)
 * and polymorphism across Account subclasses.
 */
public class TestAccountSubclasses {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws AccountException {
        Account[] accounts = {
                new SavingsAccount("S001", "Alice", 30, 1000.0, "1111", 5.0),
                new CurrentAccount("C001", "Bob", 40, 500.0, "2222", 200.0)
        };

        System.out.println("=== Polymorphic references ===");
        for (Account acc : accounts) {
            System.out.println(acc.getAccountType() + ": " + acc.getBalance());
        }
        check("accounts[0] reports SAVINGS", accounts[0].getAccountType().equals("SAVINGS"));
        check("accounts[1] reports CURRENT", accounts[1].getAccountType().equals("CURRENT"));
        check("Both are Accounts", accounts[0] instanceof Account && accounts[1] instanceof Account);

        System.out.println("\n=== Savings: interest ===");
        SavingsAccount sa = (SavingsAccount) accounts[0];
        sa.applyInterest();
        System.out.println("After interest: " + sa.getBalance());
        check("5% interest on 1000 gives 1050", Math.abs(sa.getBalance() - 1050.0) < 1e-9);
        check("Interest rate getter", sa.getInterestRate() == 5.0);

        System.out.println("\n=== Savings: inherited minimum balance rule ===");
        try {
            sa.withdraw(600); // 1050 - 600 = 450 < 500
            check("Should have thrown MinimumBalanceViolationException", false);
        } catch (MinimumBalanceViolationException e) {
            check("Minimum balance enforced: " + e.getMessage(), true);
        }
        check("canWithdraw(500) true", sa.canWithdraw(500));
        check("canWithdraw(600) false", !sa.canWithdraw(600));

        System.out.println("\n=== Current: overdraft ===");
        CurrentAccount ca = (CurrentAccount) accounts[1];
        check("Overdraft limit getter", ca.getOverdraftLimit() == 200.0);
        check("canWithdraw(700) true (balance 500 + overdraft 200)", ca.canWithdraw(700));
        ca.withdraw(650);
        System.out.println("After withdrawing 650: " + ca.getBalance());
        check("Balance goes negative within overdraft (-150)", ca.getBalance() == -150.0);
        try {
            ca.withdraw(100); // would reach -250, beyond -200
            check("Should have thrown InsufficientBalanceException", false);
        } catch (InsufficientBalanceException e) {
            check("Overdraft limit enforced: " + e.getMessage(), true);
        }
        check("Balance unchanged after rejected withdrawal", ca.getBalance() == -150.0);
        ca.deposit(300);
        check("Deposit restores positive balance (150)", ca.getBalance() == 150.0);

        System.out.println("\n=== Dynamic dispatch of overridden withdraw ===");
        Account asBase = new CurrentAccount("C002", "Dave", 28, 600.0, "3333", 400.0);
        asBase.withdraw(900); // only legal because the CurrentAccount override runs
        check("Base-typed reference runs CurrentAccount.withdraw (balance -300)", asBase.getBalance() == -300.0);

        Account savingsAsBase = new SavingsAccount("S002", "Eve", 35, 600.0, "4444", 4.0);
        try {
            savingsAsBase.withdraw(200);
            check("Savings via base reference should enforce minimum", false);
        } catch (MinimumBalanceViolationException e) {
            check("Base-typed reference runs inherited Account.withdraw for savings", true);
        }

        System.out.println("\nResult: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(String label, boolean ok) {
        if (ok) passed++; else failed++;
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + label);
    }
}
