package com.gdb.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gdb.command.DepositCommand;
import com.gdb.command.TransactionCommand;
import com.gdb.command.WithdrawCommand;
import com.gdb.domain.AccountType;
import com.gdb.domain.IAccount;
import com.gdb.support.Fixtures;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionLogTest {

    private Path dir;
    private Path file;

    @BeforeEach
    void setUp() throws Exception {
        dir = Files.createTempDirectory("gdb-log");
        file = dir.resolve("tx.log");
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(file);
        Files.deleteIfExists(dir.resolve("sub").resolve("tx.log"));
        Files.deleteIfExists(dir.resolve("sub"));
        Files.delete(dir);
    }

    private TransactionCommand deposit(IAccount a, double amount) throws Exception {
        DepositCommand c = new DepositCommand(a, amount);
        c.execute();
        return c;
    }

    @Test
    void missingFileReadsAsEmpty() throws Exception {
        assertTrue(new TransactionLog(file).readAll().isEmpty());
    }

    @Test
    void appendsAcrossSavesAndAcrossInstances() throws Exception {
        IAccount a = Fixtures.account(AccountType.SAVINGS, 1000);
        new TransactionLog(file).save(deposit(a, 10));
        new TransactionLog(file).save(deposit(a, 20));
        TransactionLog log = new TransactionLog(file);
        log.save(deposit(a, 30));

        List<TransactionCommand> all = log.readAll();
        assertEquals(3, all.size());
        assertEquals(10.0, all.get(0).getTransaction().getAmount());
        assertEquals(30.0, all.get(2).getTransaction().getAmount());
    }

    @Test
    void neverWritesThePinOrTheHoldersName() throws Exception {
        IAccount a = Fixtures.account(AccountType.SAVINGS, 1000);
        WithdrawCommand c = new WithdrawCommand(a, 100, Fixtures.PIN);
        c.execute();
        new TransactionLog(file).save(c);
        String raw = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
        assertFalse(raw.contains("pin")); // transient: not even the field name is written
        assertFalse(raw.contains("Test Holder"));
        assertTrue(raw.contains(a.getAccountNumber()));
    }

    @Test
    void createsMissingParentDirectories() throws Exception {
        Path nested = dir.resolve("sub").resolve("tx.log");
        new TransactionLog(nested).save(deposit(Fixtures.account(AccountType.SAVINGS, 1000), 5));
        assertTrue(Files.exists(nested));
    }

    @Test
    void clearEmptiesTheLog() throws Exception {
        TransactionLog log = new TransactionLog(file);
        log.save(deposit(Fixtures.account(AccountType.SAVINGS, 1000), 5));
        log.clear();
        assertTrue(log.readAll().isEmpty());
        log.clear(); // clearing an already empty log is fine
        log.save(deposit(Fixtures.account(AccountType.SAVINGS, 1000), 6));
        assertEquals(1, log.readAll().size());
    }

    @Test
    void garbageInTheFileIsAnIOException() throws Exception {
        Files.write(file, "this is not a log".getBytes(StandardCharsets.UTF_8));
        assertThrows(IOException.class, () -> new TransactionLog(file).readAll());
    }

    @Test
    void classesOutsideTheAllowListAreRejected() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(new java.util.ArrayList<>(List.of("sneaky")));
        }
        Files.write(file, bytes.toByteArray());
        assertThrows(IOException.class, () -> new TransactionLog(file).readAll());
    }
}
