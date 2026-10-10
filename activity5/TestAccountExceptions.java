/**
 * Activity 6 - Verifies every custom exception is thrown in the right
 * scenario, with a meaningful message, using try-catch blocks.
 */
public class TestAccountExceptions {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        Account acc = new Account("A001", "Alice", 30, 1000.0, "1234");

        System.out.println("=== InvalidAmountException ===");
        try {
            acc.deposit(0);
            check("Deposit of 0 should throw", false);
        } catch (InvalidAmountException e) {
            report(e, "greater than zero");
        } catch (AccountException e) {
            check("Wrong exception type: " + e.getClass().getSimpleName(), false);
        }
        try {
            acc.withdraw(-50);
            check("Withdraw of -50 should throw", false);
        } catch (InvalidAmountException e) {
            report(e, "-50.0");
        } catch (AccountException e) {
            check("Wrong exception type: " + e.getClass().getSimpleName(), false);
        }

        System.out.println("\n=== InsufficientBalanceException ===");
        try {
            acc.withdraw(5000);
            check("Withdraw of 5000 should throw", false);
        } catch (InsufficientBalanceException e) {
            report(e, "Insufficient balance");
        } catch (AccountException e) {
            check("Wrong exception type: " + e.getClass().getSimpleName(), false);
        }

        System.out.println("\n=== MinimumBalanceViolationException ===");
        try {
            acc.withdraw(700); // 1000 - 700 = 300 < 500
            check("Withdraw of 700 should throw", false);
        } catch (MinimumBalanceViolationException e) {
            report(e, "minimum of 500.0");
        } catch (AccountException e) {
            check("Wrong exception type: " + e.getClass().getSimpleName(), false);
        }
        check("Balance unchanged after failed operations", acc.getBalance() == 1000.0);

        System.out.println("\n=== InvalidPinException ===");
        try {
            acc.verifyPin("0000");
            check("Wrong PIN should throw", false);
        } catch (InvalidPinException e) {
            report(e, "Invalid PIN");
        }
        try {
            acc.closeAccount("9999");
            check("Close with wrong PIN should throw", false);
        } catch (InvalidPinException e) {
            report(e, "Invalid PIN");
        } catch (AccountException e) {
            check("Wrong exception type: " + e.getClass().getSimpleName(), false);
        }
        check("Account still ACTIVE after bad close", acc.isActive());

        System.out.println("\n=== InactiveAccountException ===");
        try {
            acc.closeAccount("1234");
            acc.deposit(100);
            check("Deposit on closed account should throw", false);
        } catch (InactiveAccountException e) {
            report(e, "CLOSED");
        } catch (AccountException e) {
            check("Wrong exception type: " + e.getClass().getSimpleName(), false);
        }
        try {
            acc.withdraw(10);
            check("Withdraw on closed account should throw", false);
        } catch (InactiveAccountException e) {
            report(e, "not active");
        } catch (AccountException e) {
            check("Wrong exception type: " + e.getClass().getSimpleName(), false);
        }

        System.out.println("\n=== Happy path after reopen ===");
        try {
            acc.reopenAccount("1234");
            acc.deposit(250);
            acc.withdraw(250);
            check("Reopen, deposit and withdraw succeed without exceptions", acc.getBalance() == 1000.0);
        } catch (AccountException e) {
            check("Unexpected exception: " + e.getMessage(), false);
        }

        System.out.println("\n=== Hierarchy ===");
        check("All specific exceptions extend AccountException",
                new InvalidAmountException(1) instanceof AccountException
                        && new InsufficientBalanceException(1, 2) instanceof AccountException
                        && new MinimumBalanceViolationException(1, 2, 3) instanceof AccountException
                        && new InactiveAccountException("X", AccountStatus.CLOSED) instanceof AccountException
                        && new InvalidPinException() instanceof AccountException);

        System.out.println("\nResult: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void report(AccountException e, String expectedFragment) {
        System.out.println("Caught " + e.getClass().getSimpleName() + ": " + e.getMessage());
        check(e.getClass().getSimpleName() + " message contains \"" + expectedFragment + "\"",
                e.getMessage().contains(expectedFragment));
    }

    private static void check(String label, boolean ok) {
        if (ok) passed++; else failed++;
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + label);
    }
}
