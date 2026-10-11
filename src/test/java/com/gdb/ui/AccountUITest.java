package com.gdb.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gdb.service.AccountService;
import com.gdb.support.Fixtures;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Drives the console UI with scripted input and checks what it prints. */
class AccountUITest {

    private final AccountService service = Fixtures.newService();

    private String run(String... lines) {
        String input = String.join("\n", lines) + "\n";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new AccountUI(service, new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(bytes, true, StandardCharsets.UTF_8)).start();
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private static final String[] OPEN = {"1", "savings", "Asha", "30", "0", "10000", "1234"};

    private static String[] concat(String[] a, String... b) {
        String[] r = new String[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    @Test
    void opensAnAccountAndExits() {
        String out = run(concat(OPEN, "0"));
        assertTrue(out.contains("Account opened. Your account number is GDB1001"));
        assertTrue(out.contains("Goodbye!"));
        assertEquals(1, service.getAllAccounts().size());
    }

    @Test
    void depositsAndWithdraws() {
        String out = run(concat(OPEN, "2", "GDB1001", "500", "3", "GDB1001", "200", "1234", "0"));
        assertTrue(out.contains("Deposited 500.00. New balance: 10500.00"));
        assertTrue(out.contains("Withdrew 200.00. New balance: 10300.00"));
    }

    @Test
    void showsBalanceAndHistory() {
        String out = run(concat(OPEN, "2", "GDB1001", "100", "5", "GDB1001", "1234", "7", "GDB1001", "1234", "0"));
        assertTrue(out.contains("Balance: 10100.00"));
        assertTrue(out.contains("Transactions for GDB1001:"));
        assertTrue(out.contains("DEPOSIT"));
    }

    @Test
    void transfersBetweenAccounts() {
        String out = run(concat(OPEN, "1", "current", "Ben", "30", "0", "0", "4321",
                "4", "GDB1001", "GDB1002", "1000", "1234", "0"));
        assertTrue(out.contains("Transferred 1000.00 from GDB1001 to GDB1002"));
    }

    @Test
    void closingNeedsConfirmation() {
        String out = run(concat(OPEN, "6", "GDB1001", "1234", "no", "0"));
        assertTrue(out.contains("Cancelled"));
        assertTrue(service.getAllAccounts().get(0).isActive());

        out = run("6", "GDB1001", "1234", "YES", "0");
        assertTrue(out.contains("is now closed"));
    }

    @Test
    void reportsErrorsAndKeepsRunning() {
        String out = run(concat(OPEN, "3", "GDB1001", "999999", "1234", "3", "GDB1001", "5", "0000", "2", "GDB1001", "lots", "9", "0"));
        assertTrue(out.contains("Error: "));
        assertTrue(out.contains("'lots' is not a valid amount"));
        assertTrue(out.contains("Invalid choice '9'"));
        assertTrue(out.contains("Goodbye!"));
        assertEquals(10000.0, service.getAllAccounts().get(0).getBalance());
    }

    @Test
    void badNumbersAbortTheOpeningFlow() {
        String out = run("1", "savings", "Asha", "thirty", "0");
        assertTrue(out.contains("'thirty' is not a whole number"));
        assertTrue(service.getAllAccounts().isEmpty());
    }

    @Test
    void endOfInputExitsCleanly() {
        String out = run(); // just an empty line, then end of input
        assertTrue(out.contains("Goodbye!"));
    }

    @Test
    void unknownAccountIsReported() {
        String out = run("5", "NOPE", "1234", "0");
        assertTrue(out.contains("Account not found: NOPE"));
    }
}
