import java.io.Serializable;

/**
 * Activity 17 - Command pattern: a banking operation packaged as an object.
 *
 * Commands are Serializable so they can be written to a log. Only plain data
 * (account numbers, amount, the resulting Transaction) is serialized. Live
 * account objects and PINs are transient: they never reach the log file, and
 * a command read back from a log is a history record that can no longer be
 * executed.
 */
public interface TransactionCommand extends Serializable {
    /**
     * Performs the operation once.
     *
     * @throws IllegalStateException if already executed, or restored from a log
     */
    void execute() throws AccountException;

    /** The record produced by execute(); null until the command has run successfully. */
    Transaction getTransaction();
}
