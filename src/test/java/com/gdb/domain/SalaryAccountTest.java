package com.gdb.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gdb.exception.InsufficientBalanceException;
import com.gdb.support.Fixtures;
import org.junit.jupiter.api.Test;

class SalaryAccountTest {

    private SalaryAccount account(double balance, int tenure) {
        return (SalaryAccount) Fixtures.account(AccountType.SALARY, balance, tenure);
    }

    @Test
    void opensWithZeroAndHasNoMinimumBalance() throws Exception {
        SalaryAccount account = account(0, 0);
        assertEquals(0.0, account.getMinimumBalance());
        account.deposit(30000);
        account.withdraw(30000);
        assertEquals(0.0, account.getBalance());
    }

    @Test
    void cannotBeOverdrawn() {
        SalaryAccount account = account(100, 0);
        assertThrows(InsufficientBalanceException.class, () -> account.withdraw(101));
    }

    @Test
    void monthlyCreditLimitFollowsTenure() {
        assertEquals(50000.0, account(0, 0).getMonthlyCreditLimit());
        assertEquals(100000.0, account(0, 1).getMonthlyCreditLimit());
        assertEquals(200000.0, account(0, 5).getMonthlyCreditLimit());
    }

    @Test
    void earnsInterestOnAPositiveBalanceOnly() {
        SalaryAccount empty = account(0, 0);
        empty.applyMonthlyInterest();
        assertEquals(0.0, empty.getBalance());

        SalaryAccount funded = account(12000, 0); // 3% a year
        funded.applyMonthlyInterest();
        assertEquals(12030.0, funded.getBalance(), 1e-9);
    }
}
