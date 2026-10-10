import java.io.IOException;
import java.util.List;

/**
 * Activity 18 - Bridge pattern, implementation side: where logged commands
 * are stored. TransactionLogger (the abstraction) only knows this interface.
 */
public interface LogDestination {
    void write(TransactionCommand command) throws IOException;

    List<TransactionCommand> readAll() throws IOException;

    void clear() throws IOException;

    String getDestinationName();
}
