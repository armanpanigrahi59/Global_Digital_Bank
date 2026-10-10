import java.io.IOException;
import java.util.List;

/**
 * Activity 18 - Bridge pattern, abstraction side. Callers log commands
 * through this class; where they end up (file, memory, database) is decided
 * by the LogDestination plugged in, and can be switched while running.
 */
public class TransactionLogger {
    private LogDestination destination;

    public TransactionLogger(LogDestination destination) {
        if (destination == null) {
            throw new IllegalArgumentException("A log destination is required");
        }
        this.destination = destination;
    }

    public synchronized void log(TransactionCommand command) throws IOException {
        destination.write(command);
    }

    public synchronized List<TransactionCommand> readAll() throws IOException {
        return destination.readAll();
    }

    public synchronized void clear() throws IOException {
        destination.clear();
    }

    public synchronized LogDestination getDestination() {
        return destination;
    }

    public synchronized String getDestinationName() {
        return destination.getDestinationName();
    }

    /** Switches to a new destination; earlier entries stay in the old one. */
    public synchronized void switchDestination(LogDestination newDestination) throws IOException {
        switchDestination(newDestination, false);
    }

    /**
     * Switches to a new destination, optionally copying the existing entries across first.
     * If the copy fails, the old destination stays in place.
     */
    public synchronized void switchDestination(LogDestination newDestination, boolean copyExisting)
            throws IOException {
        if (newDestination == null) {
            throw new IllegalArgumentException("A log destination is required");
        }
        if (copyExisting) {
            for (TransactionCommand command : destination.readAll()) {
                newDestination.write(command);
            }
        }
        this.destination = newDestination;
    }
}
