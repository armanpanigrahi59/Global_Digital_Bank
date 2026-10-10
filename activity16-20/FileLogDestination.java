import java.io.IOException;
import java.util.List;

/** Stores commands in a serialized file (Activity 17's TransactionLog). */
public class FileLogDestination implements LogDestination {
    private final TransactionLog log;

    public FileLogDestination(String filePath) {
        this.log = new TransactionLog(filePath);
    }

    @Override
    public void write(TransactionCommand command) throws IOException {
        log.save(command);
    }

    @Override
    public List<TransactionCommand> readAll() throws IOException {
        return log.readAll();
    }

    @Override
    public void clear() throws IOException {
        log.clear();
    }

    @Override
    public String getDestinationName() {
        return "File (" + log.getFile() + ")";
    }
}
