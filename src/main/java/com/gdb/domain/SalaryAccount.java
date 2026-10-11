package com.gdb.domain;

/** Zero-balance account for salary credits. Describes the expected monthly inflow as a limit. */
public class SalaryAccount extends AbstractAccount {
    static final String MONTHLY_CREDIT_LIMIT = "monthlyCreditLimit";

    public SalaryAccount(AccountRules rules, String accountNumber, String accountHolderName, int age,
                         double initialBalance, String pin, int tenureYears) {
        super(AccountType.SALARY, rules, accountNumber, accountHolderName, age, initialBalance, pin, tenureYears);
    }

    public double getMonthlyCreditLimit() {
        return rules().getFeatureValue(getAccountType(), getTenureYears(), MONTHLY_CREDIT_LIMIT, 0.0);
    }
}
