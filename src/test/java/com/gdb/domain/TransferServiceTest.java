package com.gdb.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gdb.exception.AccountException;
import com.gdb.exception.DailyLimitExceededException;
import com.gdb.exception.InactiveAccountException;
import com.gdb.exception.InsufficientBalanceException;
import com.gdb.exception.InvalidAmountException;
import com.gdb.exception.InvalidPinException;
import com.gdb.support.Fixtures;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TransferServiceTest {

    private LocalDate today = LocalDate.of(2026, 1, 15);
    private final TransferService service = new TransferService(Fixtures.rules(), () -> today);

    @Test
    void movesMoneyAndReportsTheTransaction() throws Exception {
        IAccount from = Fixtures.account(AccountType.SAVINGS, 5000);
        IAccount to = Fixtures.account(AccountType.CURRENT, 0);
        Transaction t = service.transfer(from, to, 1000, Fixtures.PIN);
        assertEquals(4000.0, from.getBalance());
        assertEquals(1000.0, to.getBalance());
        assertEquals(TransactionType.TRANSFER, t.getType());
        assertEquals(to.getAccountNumber(), t.getToAccountNumber());
        assertEquals(4000.0, t.getBalanceAfter());
    }

    @Test
    void wrongPinChangesNothing() {
        IAccount from = Fixtures.account(AccountType.SAVINGS, 5000);
        IAccount to = Fixtures.account(AccountType.SAVINGS, 5000);
        assertThrows(InvalidPinException.class, () -> service.transfer(from, to, 100, "0000"));
        assertEquals(5000.0, from.getBalance());
        assertEquals(5000.0, to.getBalance());
    }

    @Test
    void refusesInvalidAmountsAndSameAccount() {
        IAccount from = Fixtures.account(AccountType.SAVINGS, 5000);
        IAccount to = Fixtures.account(AccountType.SAVINGS, 5000);
        assertThrows(InvalidAmountException.class, () -> service.transfer(from, to, 0, Fixtures.PIN));
        assertThrows(InvalidAmountException.class, () -> service.transfer(from, to, -1, Fixtures.PIN));
        assertThrows(AccountException.class, () -> service.transfer(from, from, 10, Fixtures.PIN));
        assertEquals(5000.0, from.getBalance());
    }

    @Test
    void refusesInactiveSourceOrTarget() throws Exception {
        IAccount from = Fixtures.account(AccountType.SAVINGS, 5000);
        IAccount closed = Fixtures.account(AccountType.SAVINGS, 5000);
        closed.closeAccount(Fixtures.PIN);
        assertThrows(InactiveAccountException.class, () -> service.transfer(from, closed, 10, Fixtures.PIN));
        assertThrows(InactiveAccountException.class, () -> service.transfer(closed, from, 10, Fixtures.PIN));
        assertEquals(5000.0, from.getBalance());
    }

    @Test
    void sourceRulesStillApply() {
        IAccount from = Fixtures.account(AccountType.SAVINGS, 1000); // minimum 500
        IAccount to = Fixtures.account(AccountType.SAVINGS, 1000);
        assertThrows(AccountException.class, () -> service.transfer(from, to, 600, Fixtures.PIN));
        assertThrows(InsufficientBalanceException.class, () -> service.transfer(from, to, 5000, Fixtures.PIN));
        assertEquals(1000.0, from.getBalance());
        assertEquals(1000.0, to.getBalance());
    }

    @Test
    void dailyLimitAccumulatesAndResetsNextDay() throws Exception {
        IAccount from = Fixtures.account(AccountType.CURRENT, 200000); // limit 100000/day
        IAccount to = Fixtures.account(AccountType.CURRENT, 0);
        assertEquals(100000.0, service.getDailyTransferLimit(from));

        service.transfer(from, to, 60000, Fixtures.PIN);
        assertEquals(60000.0, service.getDailyTransferTotal(from));
        assertEquals(40000.0, service.getRemainingDailyLimit(from));
        assertTrue(service.canTransfer(from, 40000));
        assertFalse(service.canTransfer(from, 40001));

        assertThrows(DailyLimitExceededException.class, () -> service.transfer(from, to, 40001, Fixtures.PIN));
        service.transfer(from, to, 40000, Fixtures.PIN); // exactly the limit is allowed
        assertEquals(0.0, service.getRemainingDailyLimit(from));

        today = today.plusDays(1);
        assertEquals(0.0, service.getDailyTransferTotal(from));
        service.transfer(from, to, 50000, Fixtures.PIN);
    }

    @Test
    void failedTransfersDoNotUseUpTheLimit() {
        IAccount from = Fixtures.account(AccountType.CURRENT, 1000);
        IAccount to = Fixtures.account(AccountType.CURRENT, 0);
        assertThrows(AccountException.class, () -> service.transfer(from, to, 999999, Fixtures.PIN));
        assertEquals(0.0, service.getDailyTransferTotal(from));
    }

    @Test
    void failedDepositIsRolledBack() {
        IAccount from = Fixtures.account(AccountType.SAVINGS, 5000);
        IAccount real = Fixtures.account(AccountType.SAVINGS, 5000);
        IAccount broken = (IAccount) Proxy.newProxyInstance(IAccount.class.getClassLoader(),
                new Class<?>[] {IAccount.class}, (proxy, method, args) -> {
                    if (method.getName().equals("deposit")) {
                        throw new AccountException("deposit refused");
                    }
                    try {
                        return method.invoke(real, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
        AccountException e = assertThrows(AccountException.class, () -> service.transfer(from, broken, 1000, Fixtures.PIN));
        assertTrue(e.getMessage().contains("deposit refused"));
        assertEquals(5000.0, from.getBalance());
        assertEquals(0.0, service.getDailyTransferTotal(from));
    }

    @Test
    void rejectsMissingCollaborators() {
        assertThrows(IllegalArgumentException.class, () -> new TransferService(null));
        assertThrows(IllegalArgumentException.class, () -> new TransferService(Fixtures.rules(), null));
    }
}
