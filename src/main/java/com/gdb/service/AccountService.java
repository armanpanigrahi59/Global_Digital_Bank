package com.gdb.service;

import com.gdb.command.DepositCommand;
import com.gdb.command.TransactionCommand;
import com.gdb.command.TransferCommand;
import com.gdb.command.WithdrawCommand;
import com.gdb.domain.AccountType;
import com.gdb.domain.IAccount;
import com.gdb.domain.Transaction;
import com.gdb.domain.TransferService;
import com.gdb.exception.AccountException;
import com.gdb.exception.AccountNotFoundException;
import com.gdb.exception.TransactionLogException;
import com.gdb.factory.AccountFactory;
import com.gdb.logging.TransactionLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The single entry point for banking operations (application service).
 *
 * Clients such as the UI talk only to this class. It keeps the account
 * registry, runs each money movement as a Command, and records every
 * successful movement through the {@link TransactionLogger}. Business rules
 * (limits, minimum balances, PINs) stay in the domain; this layer orchestrates them.
 *
 * Only successful operations are logged. If logging fails after a movement
 * succeeded, a {@link TransactionLogException} is thrown whose message says so.
 */
public class AccountService {
    private static final String ACCOUNT_PREFIX = "GDB";
    private static final int FIRST_ACCOUNT_NUMBER = 1001;

    private final Map<String, IAccount> registry = new LinkedHashMap<>();
    private final AccountFactory factory;
    private final TransferService transferService;
    private final TransactionLogger logger;
    private int nextAccountNumber = FIRST_ACCOUNT_NUMBER;

    public AccountService(AccountFactory factory, TransferService transferService, TransactionLogger logger) {
        if (factory == null || transferService == null || logger == null) {
            throw new IllegalArgumentException("Factory, transfer service and logger are required");
        }
        this.factory = factory;
        this.transferService = transferService;
        this.logger = logger;
    }

    // ---- Accounts ----

    /**
     * Opens an account and registers it under a newly generated number. A failed
     * attempt registers nothing and does not use up an account number.
     *
     * @throws IllegalArgumentException if any detail is invalid (bad PIN, underage holder,
     *         opening balance below the product minimum, ...)
     */
    public synchronized IAccount openAccount(AccountType type, String holderName, int age,
                                             double initialBalance, String pin, int tenureYears) {
        String accountNumber = ACCOUNT_PREFIX + nextAccountNumber;
        IAccount account = factory.create(type, accountNumber, holderName, age, initialBalance, pin, tenureYears);
        registry.put(accountNumber, account);
        nextAccountNumber++;
        return account;
    }

    /** Closes the account (PIN required). It stays registered so its history remains available. */
    public synchronized void closeAccount(String accountNumber, String pin) throws AccountException {
        getAccount(accountNumber).closeAccount(pin);
    }

    public synchronized IAccount getAccount(String accountNumber) throws AccountNotFoundException {
        IAccount account = accountNumber == null ? null : registry.get(accountNumber.trim());
        if (account == null) {
            throw new AccountNotFoundException(accountNumber);
        }
        return account;
    }

    /** Balance enquiry, protected by the account PIN. */
    public synchronized double getBalance(String accountNumber, String pin) throws AccountException {
        IAccount account = getAccount(accountNumber);
        account.verifyPin(pin);
        return account.getBalance();
    }

    public synchronized List<IAccount> getAllAccounts() {
        return new ArrayList<>(registry.values());
    }

    // ---- Money movements ----

    public synchronized Transaction deposit(String accountNumber, double amount) throws AccountException {
        return run(new DepositCommand(getAccount(accountNumber), amount));
    }

    public synchronized Transaction withdraw(String accountNumber, double amount, String pin)
            throws AccountException {
        return run(new WithdrawCommand(getAccount(accountNumber), amount, pin));
    }

    public synchronized Transaction transfer(String fromAccountNumber, String toAccountNumber,
                                             double amount, String pin) throws AccountException {
        IAccount from = getAccount(fromAccountNumber);
        IAccount to = getAccount(toAccountNumber);
        return run(new TransferCommand(transferService, from, to, amount, pin));
    }

    // ---- History ----

    /** Transactions touching the account (as source or target), oldest first. PIN required. */
    public synchronized List<Transaction> getTransactionHistory(String accountNumber, String pin)
            throws AccountException {
        IAccount account = getAccount(accountNumber);
        account.verifyPin(pin);
        List<Transaction> history = new ArrayList<>();
        try {
            for (TransactionCommand command : logger.readAll()) {
                Transaction t = command.getTransaction();
                if (t != null && (t.getAccountNumber().equals(account.getAccountNumber())
                        || account.getAccountNumber().equals(t.getToAccountNumber()))) {
                    history.add(t);
                }
            }
        } catch (IOException e) {
            throw new TransactionLogException("Could not read the transaction log", e);
        }
        return history;
    }

    public synchronized TransactionLogger getLogger() {
        return logger;
    }

    // ---- Internals ----

    /** Executes the command and logs it only if it succeeded. */
    private Transaction run(TransactionCommand command) throws AccountException {
        command.execute(); // throws on any rule violation; nothing is logged then
        Transaction transaction = command.getTransaction();
        try {
            logger.log(command);
        } catch (IOException e) {
            throw new TransactionLogException("Transaction " + transaction.getTransactionId()
                    + " was completed but could not be logged", e);
        }
        return transaction;
    }
}
