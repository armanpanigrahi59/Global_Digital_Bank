import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Activity 18 - Tests the Bridge pattern: one logger, three destinations,
 * runtime switching.
 */
public class TestBridgeLogging {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        IAccount acc = AccountFactory.createAccount("SAVINGS", "S1", "Ann", 30, 5000, "1234");
        DepositCommand d1 = new DepositCommand(acc, 100);
        WithdrawCommand w1 = new WithdrawCommand(acc, 50, "1234");
        d1.execute();
        w1.execute();

        Path file = Files.createTempFile("gdb-bridge", ".bin");
        Files.delete(file);
        try {
            System.out.println("=== Every destination honours the same contract ===");
            SimulatedDatabase db = new SimulatedDatabase();
            LogDestination[] destinations = {
                    new MemoryLogDestination(),
                    new FileLogDestination(file.toString()),
                    new DatabaseLogDestination(db)
            };
            for (LogDestination dest : destinations) {
                TransactionLogger logger = new TransactionLogger(dest);
                logger.clear();
                String name = logger.getDestinationName();
                check(name + ": starts empty", logger.readAll().isEmpty());
                logger.log(d1);
                logger.log(w1);
                List<TransactionCommand> all = logger.readAll();
                check(name + ": two entries after two logs", all.size() == 2);
                check(name + ": order preserved", all.get(0).getTransaction().getType() == TransactionType.DEPOSIT
                        && all.get(1).getTransaction().getType() == TransactionType.WITHDRAW);
                check(name + ": transaction ids preserved", all.get(0).getTransaction().getTransactionId()
                        .equals(d1.getTransaction().getTransactionId()));
                logger.clear();
                check(name + ": empty after clear", logger.readAll().isEmpty());
            }
            check("Destination names are distinct", !destinations[0].getDestinationName().equals(destinations[1].getDestinationName())
                    && !destinations[1].getDestinationName().equals(destinations[2].getDestinationName()));

            System.out.println("\n=== Switching at runtime ===");
            MemoryLogDestination memory = new MemoryLogDestination();
            FileLogDestination fileDest = new FileLogDestination(file.toString());
            DatabaseLogDestination dbDest = new DatabaseLogDestination(db);
            fileDest.clear();
            dbDest.clear();

            TransactionLogger logger = new TransactionLogger(memory);
            logger.log(d1);
            check("Logged to memory", memory.readAll().size() == 1);

            logger.switchDestination(fileDest);
            check("Now reports the file destination", logger.getDestinationName().startsWith("File"));
            logger.log(w1);
            check("New entry went to the file", fileDest.readAll().size() == 1);
            check("Memory not touched after switching", memory.readAll().size() == 1);
            check("readAll reads from the current destination only", logger.readAll().size() == 1);

            logger.switchDestination(dbDest, true);
            check("Switch with copy brought the file entry over", dbDest.readAll().size() == 1);
            check("Database holds a row in its table", db.count(DatabaseLogDestination.TABLE) == 1);
            logger.log(d1);
            check("New entry went to the database", dbDest.readAll().size() == 2 && fileDest.readAll().size() == 1);

            System.out.println("\n=== Failure safety ===");
            try {
                new TransactionLogger(null);
                check("Null destination should be rejected", false);
            } catch (IllegalArgumentException e) {
                check("Null destination rejected in constructor", true);
            }
            try {
                logger.switchDestination(null);
                check("Null switch should be rejected", false);
            } catch (IllegalArgumentException e) {
                check("Null switch rejected, old destination kept", logger.getDestination() == dbDest);
            }
            LogDestination broken = new LogDestination() {
                public void write(TransactionCommand c) throws IOException { throw new IOException("disk full"); }
                public List<TransactionCommand> readAll() { return List.of(); }
                public void clear() { }
                public String getDestinationName() { return "Broken"; }
            };
            try {
                logger.switchDestination(broken, true);
                check("Copy into a failing destination should throw", false);
            } catch (IOException e) {
                check("Failed copy leaves the previous destination active", logger.getDestination() == dbDest);
            }

            System.out.println("\n=== SimulatedDatabase ===");
            SimulatedDatabase raw = new SimulatedDatabase();
            check("Missing table is empty", raw.selectAll("nope").isEmpty() && raw.count("nope") == 0);
            raw.insert("t", "a");
            raw.insert("t", "b");
            check("Rows kept in insertion order", raw.selectAll("t").equals(List.of("a", "b")));
            raw.selectAll("t").clear();
            check("selectAll returns a copy", raw.count("t") == 2);
            raw.deleteAll("t");
            check("deleteAll empties the table", raw.count("t") == 0);
        } finally {
            Files.deleteIfExists(file);
        }

        System.out.println("\nResult: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(String label, boolean ok) {
        if (ok) passed++; else failed++;
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + label);
    }
}
