import java.io.IOException;
import java.util.List;

/**
 * Activity 19 - Tests AccountService end to end through its public API:
 * open, deposit, withdraw, transfer, history, close, and error handling.
 */
public class TestAccountService {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        MemoryLogDestination memory = new MemoryLogDestination();
        TransactionLogger logger = new TransactionLogger(memory);
        AccountService service = new AccountService(logger);

        System.out.println("=== Opening accounts ===");
        IAccount a = service.openAccount("SAVINGS", "Ann Lee", 30, 10000, "1111", 0);
        IAccount b = service.openAccount("current", "Bo Chen", 41, 500, "2222", 1);
        IAccount c = service.openAccount("SALARY", "Cy Dunn", 25, 0, "3333", 0);
        check("Numbers generated in sequence", a.getAccountNumber().equals("GDB1001")
                && b.getAccountNumber().equals("GDB1002") && c.getAccountNumber().equals("GDB1003"));
        check("Registry holds all three", service.getAllAccounts().size() == 3);
        check("Lookup returns the same account", service.getAccount("GDB1001") == a);
        check("Lookup trims whitespace", service.getAccount(" GDB1002 ") == b);
        check("Holder name stored", a.getAccountHolderName().equals("Ann Lee"));

        System.out.println("\n=== Opening validation (nothing registered on failure) ===");
        expectBad("Bad PIN (letters)", () -> service.openAccount("SAVINGS", "X", 30, 1000, "12ab", 0));
        expectBad("Bad PIN (3 digits)", () -> service.openAccount("SAVINGS", "X", 30, 1000, "123", 0));
        expectBad("Blank name", () -> service.openAccount("SAVINGS", "  ", 30, 1000, "1234", 0));
        expectBad("Underage", () -> service.openAccount("SAVINGS", "Kid", 15, 1000, "1234", 0));
        expectBad("Unknown type", () -> service.openAccount("GOLD", "X", 30, 1000, "1234", 0));
        expectBad("Opening balance below minimum", () -> service.openAccount("SAVINGS", "X", 30, 100, "1234", 0));
        check("Failed opens registered nothing", service.getAllAccounts().size() == 3);
        IAccount d = service.openAccount("SAVINGS", "Di Fox", 33, 1000, "4444", 0);
        check("Failed opens did not burn account numbers", d.getAccountNumber().equals("GDB1004"));

        System.out.println("\n=== Deposit ===");
        Transaction t1 = service.deposit("GDB1001", 2500);
        check("Deposit returns a transaction", t1.getType() == TransactionType.DEPOSIT && t1.getAmount() == 2500);
        check("Balance 12500", a.getBalance() == 12500);
        check("Deposit was logged", memory.readAll().size() == 1);

        System.out.println("\n=== Withdraw ===");
        Transaction t2 = service.withdraw("GDB1001", 500, "1111");
        check("Withdraw returns a transaction", t2.getType() == TransactionType.WITHDRAW);
        check("Balance 12000", a.getBalance() == 12000);
        check("Withdraw was logged", memory.readAll().size() == 2);

        System.out.println("\n=== Transfer ===");
        Transaction t3 = service.transfer("GDB1001", "GDB1003", 3000, "1111");
        check("Transfer returns a transaction", t3.getType() == TransactionType.TRANSFER);
        check("Balances 9000 / 3000", a.getBalance() == 9000 && c.getBalance() == 3000);
        check("Transfer was logged once", memory.readAll().size() == 3);

        System.out.println("\n=== Failures change nothing and log nothing ===");
        int logged = memory.readAll().size();
        expect("Deposit to unknown account", AccountNotFoundException.class, () -> service.deposit("GDB9999", 10));
        expect("Withdraw with wrong PIN", InvalidPinException.class, () -> service.withdraw("GDB1001", 10, "0000"));
        expect("Withdraw too much", InsufficientBalanceException.class, () -> service.withdraw("GDB1001", 999999, "1111"));
        expect("Withdraw below minimum", MinimumBalanceViolationException.class, () -> service.withdraw("GDB1004", 600, "4444"));
        expect("Negative deposit", InvalidAmountException.class, () -> service.deposit("GDB1001", -5));
        expect("Transfer to unknown account", AccountNotFoundException.class, () -> service.transfer("GDB1001", "GDB0000", 10, "1111"));
        expect("Transfer with wrong PIN", InvalidPinException.class, () -> service.transfer("GDB1001", "GDB1003", 10, "9999"));
        expect("Transfer over daily limit", DailyLimitExceededException.class, () -> service.transfer("GDB1004", "GDB1003", 60000, "4444"));
        check("Nothing extra was logged", memory.readAll().size() == logged);
        check("Balances unchanged", a.getBalance() == 9000 && c.getBalance() == 3000 && d.getBalance() == 1000);

        System.out.println("\n=== Balance enquiry needs the PIN ===");
        check("Correct PIN returns balance", service.getBalance("GDB1001", "1111") == 9000);
        expect("Wrong PIN refused", InvalidPinException.class, () -> service.getBalance("GDB1001", "2222"));
        expect("Unknown account", AccountNotFoundException.class, () -> service.getBalance("NOPE", "1111"));

        System.out.println("\n=== Transaction history ===");
        List<Transaction> histA = service.getTransactionHistory("GDB1001", "1111");
        check("Ann sees 3 entries (deposit, withdraw, transfer out)", histA.size() == 3);
        check("History is oldest first", histA.get(0).getType() == TransactionType.DEPOSIT
                && histA.get(2).getType() == TransactionType.TRANSFER);
        List<Transaction> histC = service.getTransactionHistory("GDB1003", "3333");
        check("Cy sees only the incoming transfer", histC.size() == 1 && histC.get(0).getType() == TransactionType.TRANSFER);
        check("Bo has no history", service.getTransactionHistory("GDB1002", "2222").isEmpty());
        expect("History needs the PIN", InvalidPinException.class, () -> service.getTransactionHistory("GDB1001", "0000"));

        System.out.println("\n=== Closing an account ===");
        expect("Close with wrong PIN", InvalidPinException.class, () -> service.closeAccount("GDB1004", "0000"));
        service.closeAccount("GDB1004", "4444");
        check("Account closed", d.getStatus() == AccountStatus.CLOSED);
        check("Closed account stays in the registry", service.getAccount("GDB1004") == d);
        expect("Deposit to closed account", InactiveAccountException.class, () -> service.deposit("GDB1004", 10));
        expect("Transfer into closed account", InactiveAccountException.class, () -> service.transfer("GDB1001", "GDB1004", 10, "1111"));
        check("Closed account's balance preserved", d.getBalance() == 1000);

        System.out.println("\n=== Switching the log destination at runtime ===");
        logger.switchDestination(new DatabaseLogDestination(new SimulatedDatabase()), true);
        service.deposit("GDB1002", 100);
        check("History follows the copied entries plus new ones", service.getTransactionHistory("GDB1002", "2222").size() == 1
                && service.getTransactionHistory("GDB1001", "1111").size() == 3);

        System.out.println("\n=== A failing log does not hide a completed transaction ===");
        LogDestination broken = new LogDestination() {
            public void write(TransactionCommand cmd) throws IOException { throw new IOException("disk full"); }
            public List<TransactionCommand> readAll() throws IOException { throw new IOException("disk full"); }
            public void clear() { }
            public String getDestinationName() { return "Broken"; }
        };
        AccountService fragile = new AccountService(new TransactionLogger(broken));
        IAccount f = fragile.openAccount("SAVINGS", "Fran", 30, 1000, "5555", 0);
        try {
            fragile.deposit(f.getAccountNumber(), 50);
            check("Should report the logging failure", false);
        } catch (TransactionLogException e) {
            check("Reports a log failure: " + e.getMessage(), e.getMessage().contains("was completed but could not be logged"));
        }
        check("The deposit itself did happen", f.getBalance() == 1050);
        expect("History read failure is reported", TransactionLogException.class,
                () -> fragile.getTransactionHistory(f.getAccountNumber(), "5555"));

        System.out.println("\nResult: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private interface Action {
        void run() throws Exception;
    }

    private static void expectBad(String label, Action a) {
        try {
            a.run();
            check(label + " should be rejected", false);
        } catch (IllegalArgumentException e) {
            check(label + " rejected: " + e.getMessage(), true);
        } catch (Exception e) {
            check(label + " threw the wrong exception: " + e, false);
        }
    }

    private static void expect(String label, Class<? extends AccountException> type, Action a) {
        try {
            a.run();
            check(label + " should throw " + type.getSimpleName(), false);
        } catch (Exception e) {
            check(label + " -> " + e.getClass().getSimpleName(), e.getClass() == type);
        }
    }

    private static void check(String label, boolean ok) {
        if (ok) passed++; else failed++;
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + label);
    }
}
