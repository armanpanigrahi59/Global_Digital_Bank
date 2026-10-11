package com.gdb.ui;

import com.gdb.domain.AccountType;
import com.gdb.domain.IAccount;
import com.gdb.domain.Transaction;
import com.gdb.exception.AccountException;
import com.gdb.service.AccountService;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

/**
 * Menu-driven console UI. It only collects input, calls {@link AccountService}
 * and prints the outcome; it contains no banking rules. Input and output
 * streams are injectable so the UI can be driven by a scripted session in tests.
 */
public class AccountUI {
    private final AccountService service;
    private final Scanner scanner;
    private final PrintStream out;

    /** Thrown when the input stream ends, so the menu loop can exit cleanly. */
    private static final class EndOfInput extends RuntimeException {
        private static final long serialVersionUID = 1L;

        EndOfInput() {
            super(null, null, false, false);
        }
    }

    public AccountUI(AccountService service) {
        this(service, System.in, System.out);
    }

    public AccountUI(AccountService service, InputStream in, PrintStream out) {
        this.service = service;
        this.scanner = new Scanner(in);
        this.out = out;
    }

    public void start() {
        out.println("=== Global Digital Bank ===");
        try {
            while (true) {
                displayMainMenu();
                String choice = readString("Choose an option: ").trim();
                switch (choice) {
                    case "1" -> handleOpenAccount();
                    case "2" -> handleDeposit();
                    case "3" -> handleWithdraw();
                    case "4" -> handleTransfer();
                    case "5" -> handleViewBalance();
                    case "6" -> handleCloseAccount();
                    case "7" -> handleViewTransactions();
                    case "0" -> {
                        out.println("Goodbye!");
                        return;
                    }
                    default -> out.println("Invalid choice '" + choice + "'. Please enter a number from the menu.");
                }
            }
        } catch (EndOfInput e) {
            out.println();
            out.println("Goodbye!");
        }
    }

    private void displayMainMenu() {
        out.println();
        out.println("1. Open account");
        out.println("2. Deposit");
        out.println("3. Withdraw");
        out.println("4. Transfer");
        out.println("5. View balance");
        out.println("6. Close account");
        out.println("7. View transactions");
        out.println("0. Exit");
    }

    // ---- Handlers: each reports errors and returns to the menu ----

    private void handleOpenAccount() {
        try {
            AccountType type = AccountType.parse(readString("Account type (SAVINGS, CURRENT, FIXED, SALARY): "));
            String name = readString("Holder name: ");
            int age = readInt("Age: ");
            int tenure = readInt("Years as a customer (0 if new): ");
            double balance = readDouble("Opening balance: ");
            String pin = readString("Choose a 4-digit PIN: ");
            IAccount account = service.openAccount(type, name, age, balance, pin, tenure);
            out.println("Account opened. Your account number is " + account.getAccountNumber()
                    + " (" + account.getAccountType().getDisplayName() + ", balance " + money(account.getBalance()) + ").");
        } catch (RuntimeException e) {
            reportError(e);
        }
    }

    private void handleDeposit() {
        try {
            String accNo = readString("Account number: ");
            double amount = readDouble("Amount to deposit: ");
            Transaction t = service.deposit(accNo, amount);
            out.println("Deposited " + money(amount) + ". New balance: " + money(t.getBalanceAfter()));
        } catch (AccountException | RuntimeException e) {
            reportError(e);
        }
    }

    private void handleWithdraw() {
        try {
            String accNo = readString("Account number: ");
            double amount = readDouble("Amount to withdraw: ");
            String pin = readString("PIN: ");
            Transaction t = service.withdraw(accNo, amount, pin);
            out.println("Withdrew " + money(amount) + ". New balance: " + money(t.getBalanceAfter()));
        } catch (AccountException | RuntimeException e) {
            reportError(e);
        }
    }

    private void handleTransfer() {
        try {
            String from = readString("From account number: ");
            String to = readString("To account number: ");
            double amount = readDouble("Amount to transfer: ");
            String pin = readString("PIN for the source account: ");
            Transaction t = service.transfer(from, to, amount, pin);
            out.println("Transferred " + money(amount) + " from " + from + " to " + to
                    + ". Balance of " + from + ": " + money(t.getBalanceAfter()));
        } catch (AccountException | RuntimeException e) {
            reportError(e);
        }
    }

    private void handleViewBalance() {
        try {
            String accNo = readString("Account number: ");
            String pin = readString("PIN: ");
            double balance = service.getBalance(accNo, pin);
            IAccount account = service.getAccount(accNo);
            out.println(account.getAccountType().getDisplayName() + " account " + account.getAccountNumber()
                    + " (" + account.getAccountHolderName() + ", " + account.getStatus() + ")");
            out.println("Balance: " + money(balance));
        } catch (AccountException | RuntimeException e) {
            reportError(e);
        }
    }

    private void handleCloseAccount() {
        try {
            String accNo = readString("Account number: ");
            String pin = readString("PIN: ");
            String confirm = readString("Type YES to close account " + accNo + ": ");
            if (!confirm.trim().equalsIgnoreCase("YES")) {
                out.println("Cancelled. The account was not closed.");
                return;
            }
            service.closeAccount(accNo, pin);
            out.println("Account " + accNo + " is now closed.");
        } catch (AccountException | RuntimeException e) {
            reportError(e);
        }
    }

    private void handleViewTransactions() {
        try {
            String accNo = readString("Account number: ");
            String pin = readString("PIN: ");
            List<Transaction> history = service.getTransactionHistory(accNo, pin);
            if (history.isEmpty()) {
                out.println("No transactions yet for " + accNo + ".");
                return;
            }
            out.println("Transactions for " + accNo + ":");
            for (Transaction t : history) {
                out.println("  " + t);
            }
        } catch (AccountException | RuntimeException e) {
            reportError(e);
        }
    }

    // ---- Input helpers ----

    private int readInt(String prompt) {
        String raw = readString(prompt).trim();
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + raw + "' is not a whole number");
        }
    }

    private double readDouble(String prompt) {
        String raw = readString(prompt).trim();
        try {
            double value = Double.parseDouble(raw);
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + raw + "' is not a valid amount");
        }
    }

    private String readString(String prompt) {
        out.print(prompt);
        out.flush();
        if (!scanner.hasNextLine()) {
            throw new EndOfInput();
        }
        return scanner.nextLine();
    }

    private void reportError(Exception e) {
        out.println("Error: " + e.getMessage());
    }

    private static String money(double amount) {
        return String.format("%.2f", amount);
    }
}
