package com.gdb.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gdb.domain.AccountType;
import com.gdb.domain.IAccount;
import com.gdb.domain.TransactionType;
import com.gdb.domain.TransferService;
import com.gdb.exception.InvalidPinException;
import com.gdb.support.Fixtures;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.Test;

class CommandTest {

    private static TransactionCommand roundTrip(TransactionCommand c) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(c);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (TransactionCommand) in.readObject();
        }
    }

    @Test
    void depositExecutesOnceAndRecordsTheTransaction() throws Exception {
        IAccount account = Fixtures.account(AccountType.SAVINGS, 1000);
        DepositCommand c = new DepositCommand(account, 250);
        assertNull(c.getTransaction());
        c.execute();
        assertEquals(1250.0, account.getBalance());
        assertEquals(TransactionType.DEPOSIT, c.getTransaction().getType());
        assertEquals(1250.0, c.getTransaction().getBalanceAfter());
        assertThrows(IllegalStateException.class, c::execute);
        assertEquals(1250.0, account.getBalance());
    }

    @Test
    void withdrawChecksThePin() throws Exception {
        IAccount account = Fixtures.account(AccountType.SAVINGS, 1000);
        assertThrows(InvalidPinException.class, () -> new WithdrawCommand(account, 100, "0000").execute());
        assertEquals(1000.0, account.getBalance());
        WithdrawCommand c = new WithdrawCommand(account, 100, Fixtures.PIN);
        c.execute();
        assertEquals(900.0, account.getBalance());
        assertEquals(TransactionType.WITHDRAW, c.getTransaction().getType());
        assertThrows(IllegalStateException.class, c::execute);
    }

    @Test
    void transferMovesMoney() throws Exception {
        IAccount from = Fixtures.account(AccountType.SAVINGS, 5000);
        IAccount to = Fixtures.account(AccountType.SAVINGS, 5000);
        TransferCommand c = new TransferCommand(new TransferService(Fixtures.rules()), from, to, 1000, Fixtures.PIN);
        c.execute();
        assertEquals(4000.0, from.getBalance());
        assertEquals(6000.0, to.getBalance());
        assertEquals(to.getAccountNumber(), c.getToAccountNumber());
        assertEquals(from.getAccountNumber(), c.getFromAccountNumber());
        assertEquals(1000.0, c.getAmount());
    }

    @Test
    void serializedFormKeepsTheFactsButNotThePinOrTheAccount() throws Exception {
        IAccount account = Fixtures.account(AccountType.SAVINGS, 1000);
        WithdrawCommand c = new WithdrawCommand(account, 100, Fixtures.PIN);
        c.execute();

        WithdrawCommand restored = (WithdrawCommand) roundTrip(c);
        assertEquals(account.getAccountNumber(), restored.getAccountNumber());
        assertEquals(100.0, restored.getAmount());
        assertNotNull(restored.getTransaction());
        assertEquals(900.0, restored.getTransaction().getBalanceAfter());
        // a command read back from a log is history, not something to run again
        assertThrows(IllegalStateException.class, restored::execute);
        assertEquals(900.0, account.getBalance());
    }

    @Test
    void restoredTransferAndDepositAreNotExecutable() throws Exception {
        IAccount from = Fixtures.account(AccountType.SAVINGS, 5000);
        IAccount to = Fixtures.account(AccountType.SAVINGS, 5000);
        TransferCommand t = new TransferCommand(new TransferService(Fixtures.rules()), from, to, 100, Fixtures.PIN);
        t.execute();
        TransferCommand restoredTransfer = (TransferCommand) roundTrip(t);
        assertEquals(to.getAccountNumber(), restoredTransfer.getToAccountNumber());
        assertThrows(IllegalStateException.class, restoredTransfer::execute);

        DepositCommand d = new DepositCommand(from, 10);
        d.execute();
        DepositCommand restoredDeposit = (DepositCommand) roundTrip(d);
        assertThrows(IllegalStateException.class, restoredDeposit::execute);
        assertEquals(4910.0, from.getBalance());
    }
}
