package com.gdb.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class TransactionTest {

    @Test
    void shortConstructorDescribesItselfByType() {
        Transaction t = new Transaction(TransactionType.DEPOSIT, 250, "GDB1001");
        assertEquals(TransactionType.DEPOSIT, t.getType());
        assertEquals(250.0, t.getAmount());
        assertEquals("GDB1001", t.getAccountNumber());
        assertEquals("DEPOSIT", t.getDescription());
        assertNull(t.getToAccountNumber());
        assertNotNull(t.getTimestamp());
    }

    @Test
    void everyTransactionGetsItsOwnId() {
        Transaction a = new Transaction(TransactionType.DEPOSIT, 1, "A");
        Transaction b = new Transaction(TransactionType.DEPOSIT, 1, "A");
        assertNotEquals(a.getTransactionId(), b.getTransactionId());
    }

    @Test
    void fullConstructorKeepsTransferDetails() {
        Transaction t = new Transaction(TransactionType.TRANSFER, 40, "A", "B", 960, "rent");
        assertEquals("B", t.getToAccountNumber());
        assertEquals(960.0, t.getBalanceAfter());
        assertEquals("rent", t.getDescription());
        assertTrue(t.toString().contains("TRANSFER"));
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> new Transaction(null, 1, "A"));
        assertThrows(IllegalArgumentException.class, () -> new Transaction(TransactionType.DEPOSIT, 1, null));
        assertThrows(IllegalArgumentException.class, () -> new Transaction(TransactionType.DEPOSIT, 0, "A"));
        assertThrows(IllegalArgumentException.class, () -> new Transaction(TransactionType.DEPOSIT, -3, "A"));
        assertThrows(IllegalArgumentException.class, () -> new Transaction(TransactionType.DEPOSIT, Double.NaN, "A"));
    }
}
