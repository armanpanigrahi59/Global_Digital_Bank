package com.gdb.domain;

import java.util.Locale;

/** The products the bank offers. */
public enum AccountType {
    SAVINGS("Savings", "savings"),
    CURRENT("Current", "current"),
    FIXED_DEPOSIT("Fixed Deposit", "fixeddeposit"),
    SALARY("Salary", "salary");

    private final String displayName;
    private final String configKey;

    AccountType(String displayName, String configKey) {
        this.displayName = displayName;
        this.configKey = configKey;
    }

    /** Human-readable name for screens and reports. */
    public String getDisplayName() {
        return displayName;
    }

    /** Prefix and file name stem used by the rules .properties files (e.g. "fixeddeposit"). */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * Parses user input: case-insensitive, whitespace-trimmed; "FIXED" is accepted
     * as a shorthand for FIXED_DEPOSIT.
     *
     * @throws IllegalArgumentException if the text is null or names no product
     */
    public static AccountType parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Account type must not be null");
        }
        String normalized = text.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (normalized.equals("FIXED")) {
            return FIXED_DEPOSIT;
        }
        for (AccountType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown account type: " + text);
    }
}
