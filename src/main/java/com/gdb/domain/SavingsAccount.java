package com.gdb.domain;

/** Interest-bearing account that must keep its minimum balance. Uses all the default rules. */
public class SavingsAccount extends AbstractAccount {

    public SavingsAccount(AccountRules rules, String accountNumber, String accountHolderName, int age,
                          double initialBalance, String pin, int tenureYears) {
        super(AccountType.SAVINGS, rules, accountNumber, accountHolderName, age, initialBalance, pin, tenureYears);
    }
}
