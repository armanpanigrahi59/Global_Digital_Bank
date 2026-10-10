/**
 * Activity 20 - Starts the bank.
 *
 * Usage:  java -cp out Main [file|memory|db]
 *   file   (default) logs transactions to transactions.log in the current folder
 *   memory           keeps the log in memory only
 *   db               logs to the simulated database (in memory)
 */
public class Main {
    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0].trim().toLowerCase() : "file";
        LogDestination destination;
        if (mode.equals("file")) {
            destination = new FileLogDestination("transactions.log");
        } else if (mode.equals("memory")) {
            destination = new MemoryLogDestination();
        } else if (mode.equals("db")) {
            destination = new DatabaseLogDestination(new SimulatedDatabase());
        } else {
            System.err.println("Unknown log destination '" + args[0] + "'. Use file, memory or db.");
            System.exit(2);
            return;
        }

        TransactionLogger logger = new TransactionLogger(destination);
        AccountService service = new AccountService(logger);
        System.out.println("Logging to: " + logger.getDestinationName());
        new AccountUI(service).start();
    }
}
