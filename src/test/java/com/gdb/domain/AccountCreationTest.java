package com.gdb.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gdb.factory.AccountFactory;
import com.gdb.support.Fixtures;
import org.junit.jupiter.api.Test;

/** What makes an account valid to open, for every product. */
class AccountCreationTest {
    private final AccountFactory factory = Fixtures.factory();

    private IAccount open(AccountType type, String number, String name, int age, double balance, String pin, int tenure) {
        return factory.create(type, number, name, age, balance, pin, tenure);
    }

    @Test
    void rejectsHoldersUnder18() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> open(AccountType.SAVINGS, "A1", "Kid", 17, 1000, "1234", 0));
        assertTrue(e.getMessage().contains("at least 18"));
        open(AccountType.SAVINGS, "A1", "Adult", 18, 1000, "1234", 0); // exactly 18 is fine
    }

    @Test
    void rejectsPinsThatAreNotExactlyFourDigits() {
        for (String bad : new String[] {"123", "12345", "12ab", "abcd", "", " 123", "12 4", null}) {
            assertThrows(IllegalArgumentException.class,
                    () -> open(AccountType.SAVINGS, "A1", "Ann", 30, 1000, bad, 0));
        }
    }

    @Test
    void rejectsBlankOrMissingNameAndNumber() {
        assertThrows(IllegalArgumentException.class, () -> open(AccountType.SAVINGS, "A1", "  ", 30, 1000, "1234", 0));
        assertThrows(IllegalArgumentException.class, () -> open(AccountType.SAVINGS, "A1", null, 30, 1000, "1234", 0));
        assertThrows(IllegalArgumentException.class, () -> open(AccountType.SAVINGS, " ", "Ann", 30, 1000, "1234", 0));
        assertThrows(IllegalArgumentException.class, () -> open(AccountType.SAVINGS, null, "Ann", 30, 1000, "1234", 0));
    }

    @Test
    void rejectsNegativeTenure() {
        assertThrows(IllegalArgumentException.class, () -> open(AccountType.SAVINGS, "A1", "Ann", 30, 1000, "1234", -1));
    }

    @Test
    void rejectsNegativeAndNonNumericOpeningBalances() {
        assertThrows(IllegalArgumentException.class, () -> open(AccountType.SALARY, "A1", "Ann", 30, -1, "1234", 0));
        assertThrows(IllegalArgumentException.class, () -> open(AccountType.SALARY, "A1", "Ann", 30, Double.NaN, "1234", 0));
        assertThrows(IllegalArgumentException.class,
                () -> open(AccountType.SALARY, "A1", "Ann", 30, Double.POSITIVE_INFINITY, "1234", 0));
    }

    @Test
    void enforcesEachProductsMinimumOpeningBalance() {
        assertThrows(IllegalArgumentException.class, () -> open(AccountType.SAVINGS, "A1", "Ann", 30, 499.99, "1234", 0));
        assertThrows(IllegalArgumentException.class, () -> open(AccountType.FIXED_DEPOSIT, "A1", "Ann", 30, 4999, "1234", 0));
        // exactly the minimum is accepted; current and salary accounts may open empty
        assertEquals(500.0, open(AccountType.SAVINGS, "A2", "Ann", 30, 500, "1234", 0).getBalance());
        assertEquals(5000.0, open(AccountType.FIXED_DEPOSIT, "A3", "Ann", 30, 5000, "1234", 0).getBalance());
        assertEquals(0.0, open(AccountType.CURRENT, "A4", "Ann", 30, 0, "1234", 0).getBalance());
        assertEquals(0.0, open(AccountType.SALARY, "A5", "Ann", 30, 0, "1234", 0).getBalance());
    }

    @Test
    void minimumOpeningBalanceDependsOnTenure() {
        // savings minimum is 500 for a new customer but 2000 after 3 years
        assertThrows(IllegalArgumentException.class, () -> open(AccountType.SAVINGS, "A1", "Ann", 30, 1500, "1234", 3));
        open(AccountType.SAVINGS, "A2", "Ann", 30, 2000, "1234", 3);
        open(AccountType.SAVINGS, "A3", "Ann", 30, 1500, "1234", 2);
    }

    @Test
    void trimsNumberAndNameAndStartsActive() {
        IAccount account = open(AccountType.SAVINGS, " A1 ", "  Ann Lee ", 30, 1000, "1234", 0);
        assertEquals("A1", account.getAccountNumber());
        assertEquals("Ann Lee", account.getAccountHolderName());
        assertEquals(AccountStatus.ACTIVE, account.getStatus());
        assertTrue(account.isActive());
    }

    @Test
    void failedConstructionNeedsRulesAndType() {
        assertThrows(IllegalArgumentException.class, () -> new SavingsAccount(null, "A1", "Ann", 30, 1000, "1234", 0));
        assertThrows(IllegalArgumentException.class, () -> new AccountFactory(null));
    }
}
