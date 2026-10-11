package com.gdb.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gdb.exception.AccountException;
import com.gdb.exception.InactiveAccountException;
import com.gdb.exception.InvalidPinException;
import com.gdb.support.Fixtures;
import org.junit.jupiter.api.Test;

/** PIN checks and the ACTIVE / INACTIVE / CLOSED lifecycle, shared by every product. */
class AccountLifecycleTest {

    private IAccount account() {
        return Fixtures.account(AccountType.SAVINGS, 1000);
    }

    @Test
    void verifiesThePin() throws Exception {
        IAccount account = account();
        account.verifyPin(Fixtures.PIN); // does not throw
        assertThrows(InvalidPinException.class, () -> account.verifyPin("0000"));
        assertThrows(InvalidPinException.class, () -> account.verifyPin(null));
        assertThrows(InvalidPinException.class, () -> account.verifyPin(""));
    }

    @Test
    void closingNeedsTheCorrectPin() {
        IAccount account = account();
        assertThrows(InvalidPinException.class, () -> account.closeAccount("9999"));
        assertTrue(account.isActive());
    }

    @Test
    void aClosedAccountRefusesAllTransactions() throws Exception {
        IAccount account = account();
        account.closeAccount(Fixtures.PIN);
        assertEquals(AccountStatus.CLOSED, account.getStatus());
        assertFalse(account.isActive());

        InactiveAccountException e = assertThrows(InactiveAccountException.class, () -> account.deposit(10));
        assertTrue(e.getMessage().contains("CLOSED"));
        assertThrows(InactiveAccountException.class, () -> account.withdraw(10));
        assertThrows(InactiveAccountException.class, () -> account.depositWithTransaction(10));
        assertEquals(1000.0, account.getBalance());
    }

    @Test
    void closingTwiceIsRefused() throws Exception {
        IAccount account = account();
        account.closeAccount(Fixtures.PIN);
        assertThrows(InactiveAccountException.class, () -> account.closeAccount(Fixtures.PIN));
    }

    @Test
    void aClosedAccountCanBeReopened() throws Exception {
        IAccount account = account();
        account.closeAccount(Fixtures.PIN);
        assertThrows(InvalidPinException.class, () -> account.reopenAccount("9999"));
        assertEquals(AccountStatus.CLOSED, account.getStatus());

        account.reopenAccount(Fixtures.PIN);
        assertTrue(account.isActive());
        account.deposit(100);
        assertEquals(1100.0, account.getBalance());
    }

    @Test
    void reopeningAnActiveAccountIsAnError() {
        IAccount account = account();
        AccountException e = assertThrows(AccountException.class, () -> account.reopenAccount(Fixtures.PIN));
        assertTrue(e.getMessage().contains("already active"));
    }

    @Test
    void anAccountCanBeMadeInactiveAndReactivated() throws Exception {
        IAccount account = account();
        account.deactivateAccount(Fixtures.PIN);
        assertEquals(AccountStatus.INACTIVE, account.getStatus());
        assertThrows(InactiveAccountException.class, () -> account.deposit(10));
        assertThrows(InactiveAccountException.class, () -> account.deactivateAccount(Fixtures.PIN));

        account.reopenAccount(Fixtures.PIN);
        assertTrue(account.isActive());
    }

    @Test
    void aClosedAccountCannotBeMadeInactive() throws Exception {
        IAccount account = account();
        account.closeAccount(Fixtures.PIN);
        assertThrows(InactiveAccountException.class, () -> account.deactivateAccount(Fixtures.PIN));
    }

    @Test
    void anInactiveAccountCanBeClosed() throws Exception {
        IAccount account = account();
        account.deactivateAccount(Fixtures.PIN);
        account.closeAccount(Fixtures.PIN);
        assertEquals(AccountStatus.CLOSED, account.getStatus());
    }

    @Test
    void toStringNamesTheProductNumberAndStatus() {
        IAccount account = account();
        String text = account.toString();
        assertTrue(text.contains("Savings") && text.contains(account.getAccountNumber()) && text.contains("ACTIVE"));
    }
}
