import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Activity 20 - Drives the console UI with scripted keystrokes and checks
 * what it prints and what the underlying service ends up holding.
 */
public class TestAccountUI {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Full happy-path session ===");
        AccountService service = newService();
        String out = session(service,
                "1", "SAVINGS", "Ann Lee", "30", "0", "10000", "1111",   // open GDB1001
                "1", "SALARY", "Bo Chen", "28", "0", "0", "2222",        // open GDB1002
                "2", "GDB1001", "500",                                   // deposit
                "3", "GDB1001", "200", "1111",                           // withdraw
                "4", "GDB1001", "GDB1002", "1000", "1111",               // transfer
                "5", "GDB1001", "1111",                                  // balance
                "7", "GDB1001", "1111",                                  // history
                "6", "GDB1002", "2222", "YES",                           // close
                "0");
        check("Shows the menu", out.contains("1. Open account") && out.contains("0. Exit"));
        check("Reports opened account numbers", out.contains("GDB1001") && out.contains("GDB1002"));
        check("Deposit confirmed with new balance 10500.00", out.contains("Deposited 500.00. New balance: 10500.00"));
        check("Withdraw confirmed with new balance 10300.00", out.contains("Withdrew 200.00. New balance: 10300.00"));
        check("Transfer confirmed", out.contains("Transferred 1000.00 from GDB1001 to GDB1002"));
        check("Balance view shows 9300.00", out.contains("Balance: 9300.00"));
        check("History lists three transactions", out.contains("DEPOSIT") && out.contains("WITHDRAW") && out.contains("TRANSFER"));
        check("Close confirmed", out.contains("Account GDB1002 is now closed."));
        check("Says goodbye on exit", out.contains("Goodbye!"));
        check("Service state matches (9300 / 1000)", service.getAccount("GDB1001").getBalance() == 9300
                && service.getAccount("GDB1002").getBalance() == 1000);
        check("Closed account really is closed", service.getAccount("GDB1002").getStatus() == AccountStatus.CLOSED);
        check("Three movements were logged", service.getLogger().readAll().size() == 3);

        System.out.println("\n=== Errors are reported and the menu continues ===");
        service = newService();
        service.openAccount("SAVINGS", "Ann", 30, 1000, "1111", 0);
        out = session(service,
                "9",                                          // invalid menu choice
                "abc",                                        // not a number at all
                "",                                           // empty line
                "2", "GDB1001", "lots",                       // non-numeric amount
                "2", "GDB9999", "10",                         // unknown account
                "3", "GDB1001", "10", "0000",                 // wrong PIN
                "3", "GDB1001", "900", "1111",                // breaks minimum balance
                "2", "GDB1001", "-5",                         // negative amount
                "4", "GDB1001", "GDB1001", "10", "1111",      // same account
                "1", "SAVINGS", "Kid", "15", "0", "1000", "1234",   // underage
                "1", "SAVINGS", "Pat", "30", "0", "1000", "12",     // bad PIN
                "1", "SAVINGS", "Pat", "thirty",              // non-numeric age aborts at once, back to menu
                "5", "GDB1001", "1111",                       // still works afterwards
                "0");
        check("Invalid choice reported", out.contains("Invalid choice '9'") && out.contains("Invalid choice 'abc'"));
        check("Empty choice reported", out.contains("Invalid choice ''"));
        check("Bad amount reported", out.contains("Error: 'lots' is not a valid amount"));
        check("Unknown account reported", out.contains("Error: Account not found: GDB9999"));
        check("Wrong PIN reported", out.contains("Error: Invalid PIN"));
        check("Minimum balance reported", out.contains("Error: Minimum balance violation"));
        check("Negative amount reported", out.contains("Error: Invalid amount: -5.0"));
        check("Same-account transfer reported", out.contains("Error: Cannot transfer to the same account"));
        check("Underage reported", out.contains("Error: Account holder must be at least 18"));
        check("Bad PIN at opening reported", out.contains("Error: PIN must be exactly 4 digits"));
        check("Bad age reported", out.contains("Error: 'thirty' is not a whole number"));
        check("Menu still works after all those errors", out.contains("Balance: 1000.00"));
        check("None of the failures changed the balance", service.getAccount("GDB1001").getBalance() == 1000);
        check("No failed account was registered", service.getAllAccounts().size() == 1);
        check("Nothing was logged for failures", service.getLogger().readAll().isEmpty());

        System.out.println("\n=== Close needs explicit confirmation ===");
        service = newService();
        service.openAccount("SAVINGS", "Ann", 30, 1000, "1111", 0);
        out = session(service, "6", "GDB1001", "1111", "no", "0");
        check("Declining cancels the close", out.contains("Cancelled") && service.getAccount("GDB1001").isActive());

        System.out.println("\n=== Empty history ===");
        out = session(service, "7", "GDB1001", "1111", "0");
        check("Empty history message", out.contains("No transactions yet for GDB1001"));

        System.out.println("\n=== Input ending without choosing Exit ===");
        out = session(service, "2", "GDB1001");  // stream ends mid-prompt
        check("Exits cleanly at end of input", out.contains("Goodbye!"));
        out = session(service);                   // no input at all
        check("Exits cleanly with no input", out.contains("Goodbye!"));

        System.out.println("\nResult: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static AccountService newService() {
        return new AccountService(new TransactionLogger(new MemoryLogDestination()));
    }

    /** Runs the UI against the given lines of input and returns everything it printed. */
    private static String session(AccountService service, String... lines) {
        String input = lines.length == 0 ? "" : String.join("\n", lines) + "\n";
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        new AccountUI(service, new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), out).start();
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static void check(String label, boolean ok) {
        if (ok) passed++; else failed++;
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + label);
    }
}
