package com.gdb.logging;

import com.gdb.command.TransactionCommand;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Stores commands in a serialized file (see {@link TransactionLog}). */
public class FileLogDestination implements LogDestination {
    private final TransactionLog log;

    public FileLogDestination(Path file) {
        this.log = new TransactionLog(file);
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
