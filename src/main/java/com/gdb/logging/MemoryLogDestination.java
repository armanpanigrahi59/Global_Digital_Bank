package com.gdb.logging;

import com.gdb.command.TransactionCommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Keeps commands in memory only; gone when the program exits. Handy for tests and demos. */
public class MemoryLogDestination implements LogDestination {
    private final List<TransactionCommand> commands = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void write(TransactionCommand command) {
        commands.add(command);
    }

    @Override
    public List<TransactionCommand> readAll() {
        synchronized (commands) {
            return new ArrayList<>(commands);
        }
    }

    @Override
    public void clear() {
        commands.clear();
    }

    @Override
    public String getDestinationName() {
        return "Memory";
    }
}
