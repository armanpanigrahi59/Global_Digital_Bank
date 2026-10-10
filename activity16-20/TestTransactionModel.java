/**
 * Activity 16 - Tests the Transaction model and the overloaded
 * *WithTransaction methods that coexist with the original void ones.
 */
public class TestTransactionModel {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws AccountException {
        System.out.println("=== Transaction object ===");
        Transaction t = new Transaction(TransactionType.DEPOSIT, 250.0, "A1");
        check("Type kept", t.getType() == TransactionType.DEPOSIT);
        check("Amount kept", t.getAmount() == 250.0);
        check("Account kept", t.getAccountNumber().equals("A1"));
        check("Timestamp set", t.getTimestamp() != null);
        check("Id set", t.getTransactionId() != null && t.getTransactionId().length() == 36);
        check("No target account", t.getToAccountNumber() == null);
        check("Balance after unknown (NaN)", Double.isNaN(t.getBalanceAfter()));
        Transaction t2 = new Transaction(TransactionType.DEPOSIT, 250.0, "A1");
        check("Every transaction gets a unique id", !t.getTransactionId().equals(t2.getTransactionId()));
        check("Enum has exactly DEPOSIT, WITHDRAW, TRANSFER", TransactionType.values().length == 3);

        System.out.println("\n=== Transaction validation ===");
        expectBad("Zero amount", () -> new Transaction(TransactionType.DEPOSIT, 0, "A1"));
        expectBad("Negative amount", () -> new Transaction(TransactionType.WITHDRAW, -1, "A1"));
        expectBad("Null type", () -> new Transaction(null, 5, "A1"));
        expectBad("Null account", () -> new Transaction(TransactionType.DEPOSIT, 5, null));

        System.out.println("\n=== depositWithTransaction / withdrawWithTransaction ===");
        IAccount acc = AccountFactory.createAccount("SAVINGS", "S1", "Ann", 30, 1000, "1234");
        Transaction d = acc.depositWithTransaction(500);
        check("Deposit returns a DEPOSIT transaction", d.getType() == TransactionType.DEPOSIT && d.getAmount() == 500);
        check("Deposit transaction carries account number", d.getAccountNumber().equals("S1"));
        check("Deposit transaction carries balance after (1500)", d.getBalanceAfter() == 1500);
        check("Balance updated", acc.getBalance() == 1500);

        Transaction w = acc.withdrawWithTransaction(200);
        check("Withdraw returns a WITHDRAW transaction", w.getType() == TransactionType.WITHDRAW && w.getAmount() == 200);
        check("Withdraw transaction carries balance after (1300)", w.getBalanceAfter() == 1300);

        System.out.println("\n=== Original void methods still work (coexistence) ===");
        acc.deposit(100);
        acc.withdraw(100);
        check("void deposit/withdraw unaffected", acc.getBalance() == 1300);

        System.out.println("\n=== Failed operations produce no transaction ===");
        try {
            acc.withdrawWithTransaction(5000);
            check("Should throw", false);
        } catch (InsufficientBalanceException e) {
            check("Insufficient funds throws, no transaction returned", true);
        }
        try {
            acc.depositWithTransaction(-1);
            check("Should throw", false);
        } catch (InvalidAmountException e) {
            check("Invalid amount throws, no transaction returned", true);
        }
        check("Balance unchanged after failures", acc.getBalance() == 1300);

        System.out.println("\n=== Transfer returns a transaction ===");
        IAccount target = AccountFactory.createAccount("SALARY", "P1", "Bo", 30, 0, "4321");
        TransferService service = new TransferService();
        Transaction tr = service.transfer(acc, target, 300, "1234");
        check("Transfer returns a TRANSFER transaction", tr.getType() == TransactionType.TRANSFER);
        check("Recorded on the source account", tr.getAccountNumber().equals("S1"));
        check("Records the target account", "P1".equals(tr.getToAccountNumber()));
        check("Source balance after = 1000", tr.getBalanceAfter() == 1000 && acc.getBalance() == 1000);
        check("Target credited", target.getBalance() == 300);
        System.out.println("  e.g. " + tr);
        check("toString mentions both accounts", tr.toString().contains("S1") && tr.toString().contains("P1"));

        System.out.println("\nResult: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void expectBad(String label, Runnable r) {
        try {
            r.run();
            check(label + " should be rejected", false);
        } catch (IllegalArgumentException e) {
            check(label + " rejected", true);
        }
    }

    private static void check(String label, boolean ok) {
        if (ok) passed++; else failed++;
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + label);
    }
}
