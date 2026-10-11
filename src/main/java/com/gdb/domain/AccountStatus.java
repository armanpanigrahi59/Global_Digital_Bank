package com.gdb.domain;

/** Lifecycle state of an account. Only ACTIVE accounts can transact. */
public enum AccountStatus {
    ACTIVE, INACTIVE, CLOSED
}
