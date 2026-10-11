package com.gdb.logging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A stand-in for a real database: named tables holding rows. It lets the
 * Bridge pattern be shown without JDBC or a database server.
 */
public class SimulatedDatabase {
    private final Map<String, List<Object>> tables = new HashMap<>();

    public synchronized void insert(String table, Object row) {
        tables.computeIfAbsent(table, k -> new ArrayList<>()).add(row);
    }

    /** All rows of a table in insertion order (a copy; empty if the table does not exist). */
    public synchronized List<Object> selectAll(String table) {
        return new ArrayList<>(tables.getOrDefault(table, List.of()));
    }

    public synchronized void deleteAll(String table) {
        tables.remove(table);
    }

    public synchronized int count(String table) {
        return tables.getOrDefault(table, List.of()).size();
    }
}
