package com.gdb.domain;

import com.gdb.exception.AccountException;

/**
 * Transactional account: no minimum balance, but withdrawals may go below zero
 * up to an overdraft limit that depends on the customer's tenure.
 */
public class CurrentAccount extends AbstractAccount {
    static final String OVERDRAFT_LIMIT = "overdraftLimit";

    public CurrentAccount(AccountRules rules, String accountNumber, String accountHolderName, int age,
                          double initialBalance, String pin, int tenureYears) {
        super(AccountType.CURRENT, rules, accountNumber, accountHolderName, age, initialBalance, pin, tenureYears);
    }

    public double getOverdraftLimit() {
        return rules().getFeatureValue(getAccountType(), getTenureYears(), OVERDRAFT_LIMIT, 0.0);
    }

    @Override
    protected double availableFunds() {
        return getBalance() + getOverdraftLimit();
    }

    /** The overdraft limit (via availableFunds) is the only limit; no minimum balance applies. */
    @Override
    protected void checkWithdrawalRules(double amount) throws AccountException {
        // nothing further to check
    }
}
