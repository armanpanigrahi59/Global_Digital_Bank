package com.gdb.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AccountTypeTest {

    @Test
    void parsesEachTypeCaseInsensitively() {
        assertEquals(AccountType.SAVINGS, AccountType.parse("SAVINGS"));
        assertEquals(AccountType.SAVINGS, AccountType.parse("savings"));
        assertEquals(AccountType.CURRENT, AccountType.parse("Current"));
        assertEquals(AccountType.SALARY, AccountType.parse("salary"));
        assertEquals(AccountType.FIXED_DEPOSIT, AccountType.parse("FIXED_DEPOSIT"));
    }

    @Test
    void trimsWhitespaceAndAcceptsSpacesInsteadOfUnderscore() {
        assertEquals(AccountType.SALARY, AccountType.parse("  Salary \t"));
        assertEquals(AccountType.FIXED_DEPOSIT, AccountType.parse("fixed deposit"));
    }

    @Test
    void fixedIsAShorthandForFixedDeposit() {
        assertEquals(AccountType.FIXED_DEPOSIT, AccountType.parse("FIXED"));
        assertEquals(AccountType.FIXED_DEPOSIT, AccountType.parse("fixed"));
    }

    @Test
    void rejectsUnknownBlankAndNull() {
        IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class, () -> AccountType.parse("GOLD"));
        assertEquals("Unknown account type: GOLD", unknown.getMessage());
        assertThrows(IllegalArgumentException.class, () -> AccountType.parse(""));
        assertThrows(IllegalArgumentException.class, () -> AccountType.parse("   "));
        assertThrows(IllegalArgumentException.class, () -> AccountType.parse(null));
    }

    @Test
    void exposesDisplayNamesAndConfigKeys() {
        assertEquals("Fixed Deposit", AccountType.FIXED_DEPOSIT.getDisplayName());
        assertEquals("fixeddeposit", AccountType.FIXED_DEPOSIT.getConfigKey());
        assertEquals("savings", AccountType.SAVINGS.getConfigKey());
        assertEquals(4, AccountType.values().length);
    }
}
