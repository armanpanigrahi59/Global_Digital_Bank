import java.time.LocalDate;

/**
 * Activity 15 - Tests TransferService: daily limits by tenure, running totals,
 * day rollover, failure cases and all-or-nothing behaviour.
 * Run from the activity14-15 folder so ./config resolves.
 */
public class TestTransferService {
    private static int passed = 0;
    private static int failed = 0;
    private static LocalDate fakeToday = LocalDate.of(2026, 10, 10);

    public static void main(String[] args) throws AccountException {
        TransferService service = new TransferService(() -> fakeToday);

        // New savings customer: daily limit 50,000. Long-standing (3y): 200,000.
        IAccount newSaver = AccountFactory.createAccount("SAVINGS", "S-NEW", "Ann", 30, 200000, "1111", 0);
        IAccount oldSaver = AccountFactory.createAccount("SAVINGS", "S-OLD", "Ben", 45, 500000, "2222", 3);
        IAccount target = AccountFactory.createAccount("SALARY", "P-TGT", "Cy", 28, 0, "3333", 0);

        System.out.println("=== Limits come from the rules engine ===");
        check("New savings limit 50000", service.getDailyTransferLimit(newSaver) == 50000);
        check("3-year savings limit 200000", service.getDailyTransferLimit(oldSaver) == 200000);
        check("Nothing transferred yet", service.getDailyTransferTotal(newSaver) == 0);
        check("canTransfer(50000) true", service.canTransfer(newSaver, 50000));
        check("canTransfer(50001) false", !service.canTransfer(newSaver, 50001));
        check("canTransfer(0) false", !service.canTransfer(newSaver, 0));

        System.out.println("\n=== Successful transfers accumulate ===");
        service.transfer(newSaver, target, 30000, "1111");
        check("Source debited", newSaver.getBalance() == 170000);
        check("Target credited", target.getBalance() == 30000);
        check("Daily total 30000", service.getDailyTransferTotal(newSaver) == 30000);
        check("Remaining 20000", service.getRemainingDailyLimit(newSaver) == 20000);

        System.out.println("\n=== Daily limit enforcement ===");
        try {
            service.transfer(newSaver, target, 20001, "1111");
            check("Over-limit transfer should throw", false);
        } catch (DailyLimitExceededException e) {
            check("Rejected: " + e.getMessage(), true);
        }
        check("No money moved on rejection", newSaver.getBalance() == 170000 && target.getBalance() == 30000);
        check("Total unchanged on rejection", service.getDailyTransferTotal(newSaver) == 30000);
        service.transfer(newSaver, target, 20000, "1111");
        check("Exactly reaching the limit is allowed", service.getRemainingDailyLimit(newSaver) == 0);
        try {
            service.transfer(newSaver, target, 1, "1111");
            check("Anything more should throw", false);
        } catch (DailyLimitExceededException e) {
            check("Limit exhausted", true);
        }

        System.out.println("\n=== Limits are per source account ===");
        service.transfer(oldSaver, target, 150000, "2222");
        check("Other account has its own total", service.getDailyTransferTotal(oldSaver) == 150000);
        check("First account's total unaffected", service.getDailyTransferTotal(newSaver) == 50000);

        System.out.println("\n=== New day resets the total ===");
        fakeToday = fakeToday.plusDays(1);
        check("Total resets to 0 on the next day", service.getDailyTransferTotal(newSaver) == 0);
        check("Full limit available again", service.getRemainingDailyLimit(newSaver) == 50000);
        service.transfer(newSaver, target, 10000, "1111");
        check("Transfers work again next day", service.getDailyTransferTotal(newSaver) == 10000);

        System.out.println("\n=== Validation and failure cases ===");
        expect("Wrong PIN", InvalidPinException.class, () -> service.transfer(newSaver, target, 100, "0000"));
        expect("Zero amount", InvalidAmountException.class, () -> service.transfer(newSaver, target, 0, "1111"));
        expect("Negative amount", InvalidAmountException.class, () -> service.transfer(newSaver, target, -5, "1111"));
        expect("Same account", AccountException.class, () -> service.transfer(newSaver, newSaver, 100, "1111"));

        double before = newSaver.getBalance();
        IAccount poor = AccountFactory.createAccount("SAVINGS", "S-LOW", "Dan", 30, 600, "4444", 0);
        expect("Savings minimum balance blocks transfer", MinimumBalanceViolationException.class,
                () -> service.transfer(poor, target, 200, "4444"));
        check("Failed transfer left balances alone", poor.getBalance() == 600 && newSaver.getBalance() == before);

        IAccount closedTarget = AccountFactory.createAccount("SALARY", "P-CLOSED", "Fay", 28, 0, "5555", 0);
        closedTarget.closeAccount("5555");
        double sourceBefore = newSaver.getBalance();
        expect("Closed target", InactiveAccountException.class, () -> service.transfer(newSaver, closedTarget, 100, "1111"));
        check("Source untouched when target is closed", newSaver.getBalance() == sourceBefore);
        check("Daily total untouched when target is closed", service.getDailyTransferTotal(newSaver) == 10000);

        IAccount closedSource = AccountFactory.createAccount("SALARY", "P-CS", "Gus", 28, 100, "6666", 0);
        closedSource.closeAccount("6666");
        expect("Closed source", InactiveAccountException.class, () -> service.transfer(closedSource, target, 10, "6666"));

        System.out.println("\n=== Different account types ===");
        IAccount current = AccountFactory.createAccount("CURRENT", "C-1", "Hal", 40, 1000, "7777", 0);
        service.transfer(current, target, 1800, "7777"); // overdraft of 1000 allows it
        check("Current account transfer uses overdraft (balance -800)", current.getBalance() == -800);
        expect("Beyond overdraft", InsufficientBalanceException.class, () -> service.transfer(current, target, 500, "7777"));
        IAccount fixed = AccountFactory.createAccount("FIXED", "F-1", "Ivy", 50, 8000, "8888", 0);
        expect("Fixed deposit locked until maturity", MinimumBalanceViolationException.class,
                () -> service.transfer(fixed, target, 100, "8888"));
        check("Fixed deposit limit is 25000 for a new customer", service.getDailyTransferLimit(fixed) == 25000);

        System.out.println("\nResult: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private interface Action {
        void run() throws AccountException;
    }

    private static void expect(String label, Class<? extends AccountException> type, Action action) {
        try {
            action.run();
            check(label + " should throw " + type.getSimpleName(), false);
        } catch (AccountException e) {
            check(label + " -> " + e.getClass().getSimpleName(), type.isInstance(e) && e.getClass() == type);
        }
    }

    private static void check(String label, boolean ok) {
        if (ok) passed++; else failed++;
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + label);
    }
}
