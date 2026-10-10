/**
 * Activity 4 - Scenario-based tests for AccountEnhanced.
 */
public class TestAccountEnhanced {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== Validation ===");
        try {
            new AccountEnhanced("A000", "Minor", 17, 1000.0, "1234");
            check("Age 17 rejected", false);
        } catch (IllegalArgumentException e) {
            check("Age 17 rejected: " + e.getMessage(), true);
        }
        try {
            new AccountEnhanced("A000", "Poor", 30, 100.0, "1234");
            check("Balance below minimum rejected", false);
        } catch (IllegalArgumentException e) {
            check("Balance below minimum rejected: " + e.getMessage(), true);
        }

        AccountEnhanced acc = new AccountEnhanced("A001", "Bob", 25, 1000.0, "1234");
        check("New account is ACTIVE", acc.getStatus() == AccountStatus.ACTIVE);

        System.out.println("\n=== PIN verification ===");
        check("Correct PIN accepted", acc.verifyPin("1234"));
        check("Wrong PIN rejected", !acc.verifyPin("0000"));
        acc.setPin("4321");
        check("PIN change takes effect", acc.verifyPin("4321") && !acc.verifyPin("1234"));
        acc.setPin("1234");

        System.out.println("\n=== Deposit / withdraw on ACTIVE account ===");
        check("Deposit 500 succeeds", acc.deposit(500.0));
        check("Balance is 1500", acc.getBalance() == 1500.0);
        check("Deposit of negative amount rejected", !acc.deposit(-10.0));
        check("Withdraw 200 succeeds", acc.withdraw(200.0));
        check("Balance is 1300", acc.getBalance() == 1300.0);
        check("Withdraw below minimum balance rejected", !acc.withdraw(1000.0));
        check("Balance unchanged after rejected withdrawal", acc.getBalance() == 1300.0);
        check("Withdraw down to exactly the minimum allowed", acc.withdraw(800.0));
        check("Balance is 500", acc.getBalance() == 500.0);

        System.out.println("\n=== Status transitions ===");
        check("Close with wrong PIN rejected", !acc.closeAccount("0000"));
        check("Still ACTIVE after bad close", acc.isActive());
        check("Close with correct PIN succeeds", acc.closeAccount("1234"));
        check("Status is CLOSED", acc.getStatus() == AccountStatus.CLOSED);
        check("Deposit blocked on closed account", !acc.deposit(100.0));
        check("Withdraw blocked on closed account", !acc.withdraw(10.0));
        check("Balance unchanged while closed", acc.getBalance() == 500.0);
        check("Reopen with wrong PIN rejected", !acc.reopenAccount("9999"));
        check("Reopen with correct PIN succeeds", acc.reopenAccount("1234"));
        check("Deposit works again after reopen", acc.deposit(100.0) && acc.getBalance() == 600.0);

        System.out.println("\n=== INACTIVE status ===");
        check("Deactivate succeeds", acc.deactivateAccount("1234"));
        check("Deposit blocked on inactive account", !acc.deposit(50.0));

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
