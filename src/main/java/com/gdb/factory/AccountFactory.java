package com.gdb.factory;

import com.gdb.domain.AccountRules;
import com.gdb.domain.AccountType;
import com.gdb.domain.CurrentAccount;
import com.gdb.domain.FixedDepositAccount;
import com.gdb.domain.IAccount;
import com.gdb.domain.SalaryAccount;
import com.gdb.domain.SavingsAccount;

/**
 * Factory Pattern: creates accounts by product type and hands them back as
 * {@link IAccount}, so callers never depend on the concrete classes. The
 * factory owns the {@link AccountRules} its accounts are built with.
 */
public class AccountFactory {
    private final AccountRules rules;

    public AccountFactory(AccountRules rules) {
        if (rules == null) {
            throw new IllegalArgumentException("Account rules are required");
        }
        this.rules = rules;
    }

    /**
     * @throws IllegalArgumentException if any detail is invalid (see {@code AbstractAccount})
     */
    public IAccount create(AccountType type, String accountNumber, String holderName, int age,
                           double initialBalance, String pin, int tenureYears) {
        if (type == null) {
            throw new IllegalArgumentException("Account type must not be null");
        }
        return switch (type) {
            case SAVINGS -> new SavingsAccount(rules, accountNumber, holderName, age, initialBalance, pin, tenureYears);
            case CURRENT -> new CurrentAccount(rules, accountNumber, holderName, age, initialBalance, pin, tenureYears);
            case FIXED_DEPOSIT -> new FixedDepositAccount(rules, accountNumber, holderName, age, initialBalance, pin, tenureYears);
            case SALARY -> new SalaryAccount(rules, accountNumber, holderName, age, initialBalance, pin, tenureYears);
        };
    }

    /** Same as {@link #create(AccountType, String, String, int, double, String, int)} for a type given as text. */
    public IAccount create(String typeName, String accountNumber, String holderName, int age,
                           double initialBalance, String pin, int tenureYears) {
        return create(AccountType.parse(typeName), accountNumber, holderName, age, initialBalance, pin, tenureYears);
    }
}
