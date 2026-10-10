import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Activity 17 - Tests the Command pattern and the serialized file log:
 * execution, appending several records, reading them back, and that
 * sensitive data never reaches the file.
 */
public class TestCommandLogging {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        IAccount savings = AccountFactory.createAccount("SAVINGS", "S1", "Ann", 30, 5000, "9876");
        IAccount salary = AccountFactory.createAccount("SALARY", "P1", "Bo", 30, 0, "1357");
        TransferService transfers = new TransferService();

        System.out.println("=== Commands execute operations ===");
        DepositCommand deposit = new DepositCommand(savings, 1000);
        check("Transaction is null before execute", deposit.getTransaction() == null);
        deposit.execute();
        check("Deposit executed (6000)", savings.getBalance() == 6000);
        check("Command exposes its transaction", deposit.getTransaction().getType() == TransactionType.DEPOSIT);

        WithdrawCommand withdraw = new WithdrawCommand(savings, 500, "9876");
        withdraw.execute();
        check("Withdraw executed (5500)", savings.getBalance() == 5500);
        check("Withdraw transaction recorded", withdraw.getTransaction().getType() == TransactionType.WITHDRAW);

        TransferCommand transfer = new TransferCommand(transfers, savings, salary, 1500, "9876");
        transfer.execute();
        check("Transfer executed (4000 / 1500)", savings.getBalance() == 4000 && salary.getBalance() == 1500);
        check("Transfer transaction recorded", transfer.getTransaction().getType() == TransactionType.TRANSFER);

        System.out.println("\n=== Command rules ===");
        expectIllegalState("Executing twice", deposit::execute);
        check("Double execute did not deposit again", savings.getBalance() == 4000);

        WithdrawCommand badPin = new WithdrawCommand(savings, 10, "0000");
        try {
            badPin.execute();
            check("Wrong PIN should throw", false);
        } catch (InvalidPinException e) {
            check("Wrong PIN rejected, command stays unexecuted", badPin.getTransaction() == null);
        }
        WithdrawCommand tooMuch = new WithdrawCommand(savings, 999999, "9876");
        try {
            tooMuch.execute();
            check("Overdraw should throw", false);
        } catch (InsufficientBalanceException e) {
            check("Failed command has no transaction", tooMuch.getTransaction() == null);
        }

        System.out.println("\n=== File log: save and read back ===");
        Path file = Files.createTempFile("gdb-log", ".bin");
        Files.delete(file); // start with no file at all
        try {
            TransactionLog log = new TransactionLog(file.toString());
            check("Reading a missing log gives an empty list", log.readAll().isEmpty());

            log.save(deposit);
            check("One record after first save", log.readAll().size() == 1);
            log.save(withdraw);
            log.save(transfer);
            List<TransactionCommand> restored = log.readAll();
            check("Three records after appending (stream not corrupted)", restored.size() == 3);

            check("Record 1 is the deposit", restored.get(0) instanceof DepositCommand);
            check("Record 2 is the withdrawal", restored.get(1) instanceof WithdrawCommand);
            check("Record 3 is the transfer", restored.get(2) instanceof TransferCommand);

            Transaction original = transfer.getTransaction();
            Transaction copy = restored.get(2).getTransaction();
            check("Transaction id survives", copy.getTransactionId().equals(original.getTransactionId()));
            check("Transaction data survives", copy.getAmount() == 1500 && copy.getType() == TransactionType.TRANSFER
                    && copy.getAccountNumber().equals("S1") && "P1".equals(copy.getToAccountNumber()));
            check("Timestamp survives", copy.getTimestamp().equals(original.getTimestamp()));
            check("Command fields survive", ((TransferCommand) restored.get(2)).getToAccountNumber().equals("P1")
                    && ((DepositCommand) restored.get(0)).getAmount() == 1000);

            System.out.println("\n=== Security: nothing sensitive in the file ===");
            String raw = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
            check("PIN 9876 is not in the log file", !raw.contains("9876"));
            check("Account holder name is not in the log file", !raw.contains("Ann"));

            System.out.println("\n=== Restored commands are history, not executable ===");
            expectIllegalState("Executing a restored deposit", restored.get(0)::execute);
            expectIllegalState("Executing a restored withdrawal", restored.get(1)::execute);
            expectIllegalState("Executing a restored transfer", restored.get(2)::execute);
            check("No money moved by restored commands", savings.getBalance() == 4000);

            System.out.println("\n=== Clear and corrupt file handling ===");
            log.clear();
            check("Cleared log is empty", log.readAll().isEmpty());
            Files.writeString(file, "this is not a serialized log");
            try {
                log.readAll();
                check("Garbage file should fail", false);
            } catch (IOException e) {
                check("Garbage file reported as IOException", true);
            }
        } finally {
            Files.deleteIfExists(file);
        }

        System.out.println("\nResult: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private interface Action {
        void run() throws AccountException;
    }

    private static void expectIllegalState(String label, Action a) {
        try {
            a.run();
            check(label + " should be refused", false);
        } catch (IllegalStateException e) {
            check(label + " refused: " + e.getMessage(), true);
        } catch (AccountException e) {
            check(label + " threw the wrong exception: " + e, false);
        }
    }

    private static void check(String label, boolean ok) {
        if (ok) passed++; else failed++;
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + label);
    }
}
