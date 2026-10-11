package com.gdb.command;

import com.gdb.domain.IAccount;
import com.gdb.domain.Transaction;
import com.gdb.exception.AccountException;

/** Withdraw money from an account; the PIN is checked first and is never serialized. */
public class WithdrawCommand implements TransactionCommand {
    private static final long serialVersionUID = 1L;

    private final transient IAccount account;
    private final transient String pin;
    private final String accountNumber;
    private final double amount;
    private Transaction transaction;

    public WithdrawCommand(IAccount account, double amount, String pin) {
        this.account = account;
        this.pin = pin;
        this.accountNumber = account.getAccountNumber();
        this.amount = amount;
    }

    @Override
    public void execute() throws AccountException {
        if (account == null) {
            throw new IllegalStateException("Command was restored from a log and cannot be executed");
        }
        if (transaction != null) {
            throw new IllegalStateException("Command has already been executed");
        }
        account.verifyPin(pin);
        transaction = account.withdrawWithTransaction(amount);
    }

    @Override
    public Transaction getTransaction() {
        return transaction;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public double getAmount() {
        return amount;
    }
}
