package com.gdb.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * An immutable record of one completed money movement. Serializable so it can
 * be written to the transaction log.
 *
 * accountNumber is the account the transaction happened on (the source for a
 * transfer); toAccountNumber is only set for transfers.
 */
public final class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String transactionId;
    private final TransactionType type;
    private final double amount;
    private final LocalDateTime timestamp;
    private final String accountNumber;
    private final String toAccountNumber;
    private final double balanceAfter;
    private final String description;

    public Transaction(TransactionType type, double amount, String accountNumber) {
        this(type, amount, accountNumber, null, Double.NaN, type == null ? null : type.name());
    }

    public Transaction(TransactionType type, double amount, String accountNumber,
                       String toAccountNumber, double balanceAfter, String description) {
        if (type == null || accountNumber == null) {
            throw new IllegalArgumentException("Transaction type and account number are required");
        }
        if (!(amount > 0)) {
            throw new IllegalArgumentException("Transaction amount must be positive: " + amount);
        }
        this.transactionId = UUID.randomUUID().toString();
        this.type = type;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
        this.accountNumber = accountNumber;
        this.toAccountNumber = toAccountNumber;
        this.balanceAfter = balanceAfter;
        this.description = description;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public TransactionType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    /** Target account of a transfer; null for deposits and withdrawals. */
    public String getToAccountNumber() {
        return toAccountNumber;
    }

    /** Balance of {@link #getAccountNumber()} after the transaction; NaN if unknown. */
    public double getBalanceAfter() {
        return balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(timestamp.withNano(0)).append(" | ").append(type)
                .append(" | ").append(String.format("%.2f", amount))
                .append(" | ").append(accountNumber);
        if (toAccountNumber != null) {
            sb.append(" -> ").append(toAccountNumber);
        }
        if (!Double.isNaN(balanceAfter)) {
            sb.append(" | balance ").append(String.format("%.2f", balanceAfter));
        }
        sb.append(" | id ").append(transactionId, 0, 8);
        return sb.toString();
    }
}
