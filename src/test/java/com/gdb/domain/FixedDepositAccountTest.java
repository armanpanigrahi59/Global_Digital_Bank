package com.gdb.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gdb.exception.InsufficientBalanceException;
import com.gdb.exception.WithdrawalNotAllowedException;
import com.gdb.support.Fixtures;
import org.junit.jupiter.api.Test;

class FixedDepositAccountTest {

    private FixedDepositAccount deposit(double balance, int tenure) {
        return (FixedDepositAccount) Fixtures.account(AccountType.FIXED_DEPOSIT, balance, tenure);
    }

    @Test
    void takesRateAndMinimumFromTheRules() {
        FixedDepositAccount shortTerm = deposit(5000, 0);
        assertEquals(5.0, shortTerm.getInterestRate());
        assertEquals(5000.0, shortTerm.getMinimumBalance());
        FixedDepositAccount longTerm = deposit(10000, 3);
        assertEquals(7.0, longTerm.getInterestRate());
        assertEquals(10000.0, longTerm.getMinimumBalance());
    }

    @Test
    void moneyIsLockedUntilMaturity() {
        FixedDepositAccount fd = deposit(8000, 0);
        assertFalse(fd.isMatured());
        assertFalse(fd.canWithdraw(100));
        WithdrawalNotAllowedException e = assertThrows(WithdrawalNotAllowedException.class, () -> fd.withdraw(100));
        assertTrue(e.getMessage().contains("not matured"));
        assertEquals(8000.0, fd.getBalance());
    }

    @Test
    void afterMaturityTheWholeBalanceCanBeWithdrawn() throws Exception {
        FixedDepositAccount fd = deposit(8000, 0);
        fd.markMatured();
        assertTrue(fd.isMatured());
        assertTrue(fd.canWithdraw(8000));
        fd.withdraw(8000); // the opening minimum no longer applies
        assertEquals(0.0, fd.getBalance());
    }

    @Test
    void stillCannotWithdrawMoreThanItHolds() {
        FixedDepositAccount fd = deposit(8000, 0);
        fd.markMatured();
        assertThrows(InsufficientBalanceException.class, () -> fd.withdraw(8001));
    }

    @Test
    void earnsMonthlyInterest() {
        FixedDepositAccount fd = deposit(12000, 0); // 5% a year
        fd.applyMonthlyInterest();
        assertEquals(12050.0, fd.getBalance(), 1e-9);
    }
}
