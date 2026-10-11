package com.gdb.support;

import com.gdb.domain.AccountRules;
import com.gdb.domain.AccountType;
import com.gdb.domain.IAccount;
import com.gdb.domain.TransferService;
import com.gdb.factory.AccountFactory;
import com.gdb.logging.LogDestination;
import com.gdb.logging.MemoryLogDestination;
import com.gdb.logging.TransactionLogger;
import com.gdb.rules.AccountRulesEngine;
import com.gdb.service.AccountService;

import java.util.concurrent.atomic.AtomicInteger;

/** Shared builders so each test states only what matters to it. Uses the real packaged rules. */
public final class Fixtures {
    /** PIN of every account built by {@link #account}. */
    public static final String PIN = "1234";

    private static final AtomicInteger COUNTER = new AtomicInteger(1);

    private Fixtures() {
    }

    public static AccountRules rules() {
        return AccountRulesEngine.getInstance();
    }

    public static AccountFactory factory() {
        return new AccountFactory(rules());
    }

    /** A new customer's account (tenure 0) with a unique number and PIN {@link #PIN}. */
    public static IAccount account(AccountType type, double balance) {
        return account(type, balance, 0);
    }

    public static IAccount account(AccountType type, double balance, int tenureYears) {
        return factory().create(type, "T" + COUNTER.getAndIncrement(), "Test Holder", 30, balance, PIN, tenureYears);
    }

    public static AccountService newService() {
        return newService(new MemoryLogDestination());
    }

    public static AccountService newService(LogDestination destination) {
        AccountRules rules = rules();
        return new AccountService(new AccountFactory(rules), new TransferService(rules),
                new TransactionLogger(destination));
    }
}
