package com.gdb.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gdb.command.DepositCommand;
import com.gdb.command.TransactionCommand;
import com.gdb.domain.AccountType;
import com.gdb.domain.IAccount;
import com.gdb.support.Fixtures;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The same logger works unchanged over every destination (Bridge pattern). */
class LoggerBridgeTest {

    private TransactionCommand deposit(double amount) throws Exception {
        IAccount a = Fixtures.account(AccountType.SAVINGS, 1000);
        DepositCommand c = new DepositCommand(a, amount);
        c.execute();
        return c;
    }

    private void exercise(LogDestination destination) throws Exception {
        TransactionLogger logger = new TransactionLogger(destination);
        assertTrue(logger.readAll().isEmpty());
        logger.log(deposit(1));
        logger.log(deposit(2));
        assertEquals(2, logger.readAll().size());
        assertEquals(2.0, logger.readAll().get(1).getTransaction().getAmount());
        logger.clear();
        assertTrue(logger.readAll().isEmpty());
    }

    @Test
    void worksOverMemory() throws Exception {
        exercise(new MemoryLogDestination());
    }

    @Test
    void worksOverTheSimulatedDatabase() throws Exception {
        SimulatedDatabase db = new SimulatedDatabase();
        exercise(new DatabaseLogDestination(db));
        assertEquals(0, db.count(DatabaseLogDestination.TABLE));
    }

    @Test
    void worksOverAFile() throws Exception {
        Path dir = Files.createTempDirectory("gdb-bridge");
        Path file = dir.resolve("tx.log");
        try {
            exercise(new FileLogDestination(file));
        } finally {
            Files.deleteIfExists(file);
            Files.delete(dir);
        }
    }

    @Test
    void switchingDestinationCanCopyExistingEntries() throws Exception {
        TransactionLogger logger = new TransactionLogger(new MemoryLogDestination());
        logger.log(deposit(1));
        logger.switchDestination(new MemoryLogDestination(), true);
        assertEquals(1, logger.readAll().size());
        logger.switchDestination(new MemoryLogDestination());
        assertTrue(logger.readAll().isEmpty());
        assertEquals("Memory", logger.getDestinationName());
    }

    @Test
    void databaseDestinationStoresRowsInItsTable() throws Exception {
        SimulatedDatabase db = new SimulatedDatabase();
        TransactionLogger logger = new TransactionLogger(new DatabaseLogDestination(db));
        logger.log(deposit(5));
        assertEquals(1, db.count(DatabaseLogDestination.TABLE));
        assertEquals(1, db.selectAll(DatabaseLogDestination.TABLE).size());
    }
}
