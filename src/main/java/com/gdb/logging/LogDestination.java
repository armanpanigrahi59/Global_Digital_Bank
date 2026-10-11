package com.gdb.logging;

import com.gdb.command.TransactionCommand;

import java.io.IOException;
import java.util.List;

/**
 * Bridge Pattern, implementation side: where logged commands are stored.
 * {@link TransactionLogger} (the abstraction) only knows this interface.
 */
public interface LogDestination {
    void write(TransactionCommand command) throws IOException;

    List<TransactionCommand> readAll() throws IOException;

    void clear() throws IOException;

    String getDestinationName();
}
