package com.gdb;

import com.gdb.domain.TransferService;
import com.gdb.factory.AccountFactory;
import com.gdb.logging.DatabaseLogDestination;
import com.gdb.logging.FileLogDestination;
import com.gdb.logging.LogDestination;
import com.gdb.logging.MemoryLogDestination;
import com.gdb.logging.SimulatedDatabase;
import com.gdb.logging.TransactionLogger;
import com.gdb.rules.AccountRulesEngine;
import com.gdb.service.AccountService;
import com.gdb.ui.AccountUI;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Application entry point and composition root: the one place where the
 * concrete pieces (rules engine, factory, logger, service, UI) are chosen and wired together.
 *
 * <pre>
 *   java -jar global-digital-bank.jar [file|memory|db]
 *     file    (default) transactions are logged to data/transactions.log
 *     memory  log kept in memory only
 *     db      log kept in the simulated database (in memory)
 *   -Dgdb.rules.dir=PATH   use edited rules files from PATH instead of the packaged ones
 * </pre>
 */
public final class GlobalDigitalBankApp {
    static final Path DEFAULT_LOG_FILE = Paths.get("data", "transactions.log");

    private GlobalDigitalBankApp() {
    }

    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0] : "file";
        LogDestination destination;
        try {
            destination = createDestination(mode, DEFAULT_LOG_FILE);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println("Usage: java -jar global-digital-bank.jar [file|memory|db]");
            System.exit(2);
            return;
        }
        AccountService service = createService(destination);
        System.out.println("Logging to: " + service.getLogger().getDestinationName());
        new AccountUI(service).start();
    }

    /** @throws IllegalArgumentException for an unknown mode */
    static LogDestination createDestination(String mode, Path logFile) {
        switch (mode.trim().toLowerCase()) {
            case "file":
                return new FileLogDestination(logFile);
            case "memory":
                return new MemoryLogDestination();
            case "db":
                return new DatabaseLogDestination(new SimulatedDatabase());
            default:
                throw new IllegalArgumentException("Unknown log destination '" + mode + "'. Use file, memory or db.");
        }
    }

    static AccountService createService(LogDestination destination) {
        AccountRulesEngine rules = AccountRulesEngine.getInstance();
        return new AccountService(new AccountFactory(rules), new TransferService(rules),
                new TransactionLogger(destination));
    }
}
