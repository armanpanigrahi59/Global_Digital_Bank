package com.gdb.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gdb.exception.InactiveAccountException;
import com.gdb.exception.InsufficientBalanceException;
import com.gdb.exception.InvalidAmountException;
import com.gdb.exception.MinimumBalanceViolationException;
import com.gdb.support.Fixtures;
import org.junit.jupiter.api.Test;

class SavingsAccountTest {

    @Test
    void takesItsNumbersFromTheRules() {
        IAccount newCustomer = Fixtures.account(AccountType.SAVINGS, 1000, 0);
        assertEquals(AccountType.SAVINGS, newCustomer.getAccountType());
        assertEquals(500.0, newCustomer.getMinimumBalance());
        assertEquals(2.5, newCustomer.getInterestRate());

        IAccount loyal = Fixtures.account(AccountType.SAVINGS, 5000, 3);
        assertEquals(2000.0, loyal.getMinimumBalance());
        assertEquals(4.5, loyal.getInterestRate());
        assertEquals("Premium Savings", ((AbstractAccount) loyal).getFeatureName());
    }

    @Test
    void depositIncreasesTheBalance() throws Exception {
        IAccount account = Fixtures.account(AccountType.SAVINGS, 1000);
        account.deposit(500);
        assertEquals(1500.0, account.getBalance());
    }

    @Test
    void rejectsNonPositiveAndNonFiniteAmounts() throws Exception {
        IAccount account = Fixtures.account(AccountType.SAVINGS, 1000);
        for (double bad : new double[] {0, -5, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(InvalidAmountException.class, () -> account.deposit(bad));
            assertThrows(InvalidAmountException.class, () -> account.withdraw(bad));
            assertFalse(account.canWithdraw(bad));
        }
        assertEquals(1000.0, account.getBalance());
    }

    @Test
    void withdrawalMayReachExactlyTheMinimumBalance() throws Exception {
        IAccount account = Fixtures.account(AccountType.SAVINGS, 1000);
        account.withdraw(500);
        assertEquals(500.0, account.getBalance());
    }

    @Test
    void withdrawalBelowTheMinimumIsRefusedAndChangesNothing() {
        IAccount account = Fixtures.account(AccountType.SAVINGS, 1000);
        MinimumBalanceViolationException e = assertThrows(MinimumBalanceViolationException.class,
                () -> account.withdraw(600));
        assertTrue(e.getMessage().contains("500.0"));
        assertEquals(1000.0, account.getBalance());
    }

    @Test
    void withdrawingMoreThanTheBalanceIsInsufficientFunds() {
        IAccount account = Fixtures.account(AccountType.SAVINGS, 1000);
        assertThrows(InsufficientBalanceException.class, () -> account.withdraw(5000));
        assertEquals(1000.0, account.getBalance());
    }

    @Test
    void canWithdrawAgreesWithWithdrawAndNeverChangesState() throws Exception {
        IAccount account = Fixtures.account(AccountType.SAVINGS, 1000);
        assertTrue(account.canWithdraw(500));
        assertFalse(account.canWithdraw(501));
        assertFalse(account.canWithdraw(5000));
        assertEquals(1000.0, account.getBalance());
        account.closeAccount(Fixtures.PIN);
        assertFalse(account.canWithdraw(10));
    }

    @Test
    void minimumBalanceFollowsTenure() {
        IAccount loyal = Fixtures.account(AccountType.SAVINGS, 2500, 3); // minimum 2000
        assertThrows(MinimumBalanceViolationException.class, () -> loyal.withdraw(600));
    }

    @Test
    void monthlyInterestIsOneTwelfthOfTheAnnualRate() {
        IAccount account = Fixtures.account(AccountType.SAVINGS, 1200); // 2.5% a year
        account.applyMonthlyInterest();
        assertEquals(1202.5, account.getBalance(), 1e-9);
    }

    @Test
    void noInterestOnAClosedAccount() throws Exception {
        IAccount account = Fixtures.account(AccountType.SAVINGS, 1200);
        account.closeAccount(Fixtures.PIN);
        account.applyMonthlyInterest();
        assertEquals(1200.0, account.getBalance());
        assertThrows(InactiveAccountException.class, () -> account.deposit(1));
    }
}
