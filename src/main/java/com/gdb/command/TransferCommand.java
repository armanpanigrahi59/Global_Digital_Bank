package com.gdb.command;

import com.gdb.domain.IAccount;
import com.gdb.domain.Transaction;
import com.gdb.domain.TransferService;
import com.gdb.exception.AccountException;

/** Transfer between two accounts through the TransferService (daily limits apply). */
public class TransferCommand implements TransactionCommand {
    private static final long serialVersionUID = 1L;

    private final transient TransferService transferService;
    private final transient IAccount from;
    private final transient IAccount to;
    private final transient String pin;
    private final String fromAccountNumber;
    private final String toAccountNumber;
    private final double amount;
    private Transaction transaction;

    public TransferCommand(TransferService transferService, IAccount from, IAccount to,
                           double amount, String pin) {
        this.transferService = transferService;
        this.from = from;
        this.to = to;
        this.pin = pin;
        this.fromAccountNumber = from.getAccountNumber();
        this.toAccountNumber = to.getAccountNumber();
        this.amount = amount;
    }

    @Override
    public void execute() throws AccountException {
        if (transferService == null) {
            throw new IllegalStateException("Command was restored from a log and cannot be executed");
        }
        if (transaction != null) {
            throw new IllegalStateException("Command has already been executed");
        }
        transaction = transferService.transfer(from, to, amount, pin);
    }

    @Override
    public Transaction getTransaction() {
        return transaction;
    }

    public String getFromAccountNumber() {
        return fromAccountNumber;
    }

    public String getToAccountNumber() {
        return toAccountNumber;
    }

    public double getAmount() {
        return amount;
    }
}
