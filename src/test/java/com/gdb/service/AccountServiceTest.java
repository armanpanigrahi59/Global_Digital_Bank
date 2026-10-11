package com.gdb.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gdb.domain.AccountType;
import com.gdb.domain.IAccount;
import com.gdb.domain.Transaction;
import com.gdb.domain.TransactionType;
import com.gdb.exception.AccountException;
import com.gdb.exception.AccountNotFoundException;
import com.gdb.exception.InactiveAccountException;
import com.gdb.exception.InvalidPinException;
import com.gdb.exception.TransactionLogException;
import com.gdb.logging.LogDestination;
import com.gdb.command.TransactionCommand;
import com.gdb.support.Fixtures;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountServiceTest {

    private AccountService service;
    private final String pin = Fixtures.PIN;

    @BeforeEach
    void setUp() {
        service = Fixtures.newService();
    }

    private IAccount open(AccountType type, double balance) {
        return service.openAccount(type, "Asha", 30, balance, pin, 0);
    }

    @Test
    void numbersAccountsSequentially() {
        assertEquals("GDB1001", open(AccountType.SAVINGS, 1000).getAccountNumber());
        assertEquals("GDB1002", open(AccountType.CURRENT, 0).getAccountNumber());
        assertEquals(2, service.getAllAccounts().size());
    }

    @Test
    void failedOpeningsDoNotConsumeANumber() {
        assertThrows(IllegalArgumentException.class, () -> open(AccountType.SAVINGS, 10));
        assertThrows(IllegalArgumentException.class, () -> service.openAccount(AccountType.SAVINGS, "A", 17, 1000, pin, 0));
        assertEquals("GDB1001", open(AccountType.SAVINGS, 1000).getAccountNumber());
    }

    @Test
    void unknownAccountsAreReported() {
        AccountNotFoundException e = assertThrows(AccountNotFoundException.class, () -> service.getAccount("NOPE"));
        assertTrue(e.getMessage().contains("NOPE"));
        assertThrows(AccountNotFoundException.class, () -> service.deposit("NOPE", 10));
    }

    @Test
    void depositWithdrawAndBalance() throws Exception {
        IAccount a = open(AccountType.SAVINGS, 1000);
        Transaction d = service.deposit(a.getAccountNumber(), 500);
        assertEquals(TransactionType.DEPOSIT, d.getType());
        assertEquals(1500.0, d.getBalanceAfter());
        service.withdraw(a.getAccountNumber(), 200, pin);
        assertEquals(1300.0, service.getBalance(a.getAccountNumber(), pin));
        assertThrows(InvalidPinException.class, () -> service.getBalance(a.getAccountNumber(), "0000"));
        assertThrows(InvalidPinException.class, () -> service.withdraw(a.getAccountNumber(), 1, "0000"));
    }

    @Test
    void transferBetweenAccounts() throws Exception {
        IAccount a = open(AccountType.SAVINGS, 5000);
        IAccount b = open(AccountType.CURRENT, 0);
        service.transfer(a.getAccountNumber(), b.getAccountNumber(), 1000, pin);
        assertEquals(4000.0, a.getBalance());
        assertEquals(1000.0, b.getBalance());
    }

    @Test
    void historyContainsOnlySuccessfulTransactionsOfThatAccount() throws Exception {
        IAccount a = open(AccountType.SAVINGS, 5000);
        IAccount b = open(AccountType.SAVINGS, 5000);
        service.deposit(a.getAccountNumber(), 100);
        assertThrows(AccountException.class, () -> service.withdraw(a.getAccountNumber(), 999999, pin));
        service.transfer(a.getAccountNumber(), b.getAccountNumber(), 50, pin);
        service.deposit(b.getAccountNumber(), 7);

        List<Transaction> forA = service.getTransactionHistory(a.getAccountNumber(), pin);
        assertEquals(2, forA.size());
        assertEquals(TransactionType.DEPOSIT, forA.get(0).getType());
        assertEquals(TransactionType.TRANSFER, forA.get(1).getType());
        assertEquals(2, service.getTransactionHistory(b.getAccountNumber(), pin).size()); // transfer in + deposit
        assertThrows(InvalidPinException.class, () -> service.getTransactionHistory(a.getAccountNumber(), "0000"));
    }

    @Test
    void closedAccountsRefuseWork() throws Exception {
        IAccount a = open(AccountType.SAVINGS, 1000);
        assertThrows(InvalidPinException.class, () -> service.closeAccount(a.getAccountNumber(), "0000"));
        service.closeAccount(a.getAccountNumber(), pin);
        assertThrows(InactiveAccountException.class, () -> service.deposit(a.getAccountNumber(), 10));
    }

    @Test
    void aLoggingFailureIsReportedAfterTheMoneyMoved() {
        AccountService s = Fixtures.newService(new LogDestination() {
            @Override public void write(TransactionCommand c) throws IOException { throw new IOException("disk full"); }
            @Override public List<TransactionCommand> readAll() { return List.of(); }
            @Override public void clear() { }
            @Override public String getDestinationName() { return "Broken"; }
        });
        IAccount a = s.openAccount(AccountType.SAVINGS, "Asha", 30, 1000, pin, 0);
        TransactionLogException e = assertThrows(TransactionLogException.class, () -> s.deposit(a.getAccountNumber(), 10));
        assertTrue(e.getMessage().contains("completed but could not be logged"));
        assertTrue(e.getMessage().contains("disk full"));
        assertEquals(1010.0, a.getBalance());
    }
}
