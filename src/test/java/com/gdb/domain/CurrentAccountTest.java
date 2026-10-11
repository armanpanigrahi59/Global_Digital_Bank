package com.gdb.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gdb.exception.InsufficientBalanceException;
import com.gdb.support.Fixtures;
import org.junit.jupiter.api.Test;

class CurrentAccountTest {

    private CurrentAccount account(double balance, int tenure) {
        return (CurrentAccount) Fixtures.account(AccountType.CURRENT, balance, tenure);
    }

    @Test
    void hasNoMinimumBalanceAndNoInterest() {
        CurrentAccount account = account(0, 0);
        assertEquals(0.0, account.getMinimumBalance());
        assertEquals(0.0, account.getInterestRate());
    }

    @Test
    void overdraftLimitGrowsWithTenure() {
        assertEquals(1000.0, account(0, 0).getOverdraftLimit());
        assertEquals(5000.0, account(0, 1).getOverdraftLimit());
        assertEquals(10000.0, account(0, 3).getOverdraftLimit());
    }

    @Test
    void canWithdrawIntoTheOverdraft() throws Exception {
        CurrentAccount account = account(500, 0); // 500 balance + 1000 overdraft
        assertTrue(account.canWithdraw(1500));
        account.withdraw(1300);
        assertEquals(-800.0, account.getBalance());
    }

    @Test
    void cannotGoBeyondTheOverdraftLimit() throws Exception {
        CurrentAccount account = account(500, 0);
        account.withdraw(1300);
        assertFalse(account.canWithdraw(300)); // would reach -1100
        assertThrows(InsufficientBalanceException.class, () -> account.withdraw(300));
        assertEquals(-800.0, account.getBalance());
        account.withdraw(200); // exactly reaches the -1000 limit
        assertEquals(-1000.0, account.getBalance());
    }

    @Test
    void depositsRepayTheOverdraft() throws Exception {
        CurrentAccount account = account(0, 0);
        account.withdraw(400);
        account.deposit(1000);
        assertEquals(600.0, account.getBalance());
    }

    @Test
    void earnsNoInterest() {
        CurrentAccount account = account(1000, 0);
        account.applyMonthlyInterest();
        assertEquals(1000.0, account.getBalance());
    }
}
