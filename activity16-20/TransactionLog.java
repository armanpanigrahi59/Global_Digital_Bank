import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Activity 17 - Persists executed commands to a file using Java serialization.
 *
 * Two details that matter:
 *  - Appending: each save opens the file in append mode, but a normal
 *    ObjectOutputStream writes a fresh stream header every time, which makes
 *    the file unreadable. When the file already has data we use a stream that
 *    skips the header.
 *  - Reading: deserializing a file is dangerous if the file is tampered with,
 *    so only this project's command/transaction classes and java.* types are
 *    accepted.
 */
public class TransactionLog {
    private static final Set<Class<?>> ALLOWED = Set.of(
            DepositCommand.class, WithdrawCommand.class, TransferCommand.class,
            Transaction.class, TransactionType.class);

    private final Path file;

    public TransactionLog(String filename) {
        this.file = Paths.get(filename);
    }

    public Path getFile() {
        return file;
    }

    public synchronized void save(TransactionCommand command) throws IOException {
        boolean append = Files.exists(file) && Files.size(file) > 0;
        try (FileOutputStream fos = new FileOutputStream(file.toFile(), true);
             ObjectOutputStream oos = append ? new HeaderlessObjectOutputStream(fos) : new ObjectOutputStream(fos)) {
            oos.writeObject(command);
        }
    }

    public synchronized List<TransactionCommand> readAll() throws IOException {
        List<TransactionCommand> commands = new ArrayList<>();
        if (!Files.exists(file) || Files.size(file) == 0) {
            return commands;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            ois.setObjectInputFilter(info -> {
                Class<?> c = info.serialClass();
                if (c == null || c.isArray() || c.isPrimitive()) {
                    return ObjectInputFilter.Status.UNDECIDED;
                }
                return (ALLOWED.contains(c) || c.getName().startsWith("java."))
                        ? ObjectInputFilter.Status.ALLOWED
                        : ObjectInputFilter.Status.REJECTED;
            });
            while (true) {
                try {
                    commands.add((TransactionCommand) ois.readObject());
                } catch (EOFException endOfFile) {
                    break;
                }
            }
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new IOException("Log file " + file + " is not a valid transaction log: " + e.getMessage(), e);
        }
        return commands;
    }

    public synchronized void clear() throws IOException {
        Files.deleteIfExists(file);
    }

    /** ObjectOutputStream for appending to an existing stream: writes no new header. */
    private static final class HeaderlessObjectOutputStream extends ObjectOutputStream {
        HeaderlessObjectOutputStream(OutputStream out) throws IOException {
            super(out);
        }

        @Override
        protected void writeStreamHeader() throws IOException {
            reset();
        }
    }
}
