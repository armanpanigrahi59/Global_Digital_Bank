package com.gdb.logging;

import com.gdb.command.TransactionCommand;

import java.util.ArrayList;
import java.util.List;

/** Stores commands as rows of the "transaction_log" table in a {@link SimulatedDatabase}. */
public class DatabaseLogDestination implements LogDestination {
    static final String TABLE = "transaction_log";

    private final SimulatedDatabase db;

    public DatabaseLogDestination(SimulatedDatabase db) {
        this.db = db;
    }

    @Override
    public void write(TransactionCommand command) {
        db.insert(TABLE, command);
    }

    @Override
    public List<TransactionCommand> readAll() {
        List<TransactionCommand> result = new ArrayList<>();
        for (Object row : db.selectAll(TABLE)) {
            result.add((TransactionCommand) row);
        }
        return result;
    }

    @Override
    public void clear() {
        db.deleteAll(TABLE);
    }

    @Override
    public String getDestinationName() {
        return "Database (simulated)";
    }
}
