package com.gdb.domain;

import com.gdb.exception.AccountException;
import com.gdb.exception.WithdrawalNotAllowedException;

/**
 * Term deposit: money is locked until the deposit matures, after which it can
 * be withdrawn in full (the minimum balance only applies to opening the deposit).
 */
public class FixedDepositAccount extends AbstractAccount {
    private boolean matured = false;

    public FixedDepositAccount(AccountRules rules, String accountNumber, String accountHolderName, int age,
                               double initialBalance, String pin, int tenureYears) {
        super(AccountType.FIXED_DEPOSIT, rules, accountNumber, accountHolderName, age, initialBalance, pin, tenureYears);
    }

    public boolean isMatured() {
        return matured;
    }

    /** Marks the deposit as matured. (A full system would derive this from the calendar.) */
    public void markMatured() {
        this.matured = true;
    }

    @Override
    protected void checkWithdrawalRules(double amount) throws AccountException {
        if (!matured) {
            throw new WithdrawalNotAllowedException("fixed deposit " + getAccountNumber() + " has not matured yet");
        }
    }
}
