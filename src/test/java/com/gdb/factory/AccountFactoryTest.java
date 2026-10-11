package com.gdb.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gdb.domain.AccountRules;
import com.gdb.domain.AccountType;
import com.gdb.domain.CurrentAccount;
import com.gdb.domain.FixedDepositAccount;
import com.gdb.domain.IAccount;
import com.gdb.domain.SalaryAccount;
import com.gdb.domain.SavingsAccount;
import com.gdb.support.Fixtures;
import org.junit.jupiter.api.Test;

class AccountFactoryTest {

    private final AccountFactory factory = Fixtures.factory();

    @Test
    void buildsTheRightSubclassPerType() {
        assertTrue(make(AccountType.SAVINGS) instanceof SavingsAccount);
        assertTrue(make(AccountType.CURRENT) instanceof CurrentAccount);
        assertTrue(make(AccountType.FIXED_DEPOSIT) instanceof FixedDepositAccount);
        assertTrue(make(AccountType.SALARY) instanceof SalaryAccount);
    }

    private IAccount make(AccountType type) {
        return factory.create(type, "N1", "Asha", 30, 20000, "1234", 0);
    }

    @Test
    void acceptsTheTypeAsText() {
        IAccount a = factory.create("fixed", "N1", "Asha", 30, 20000, "1234", 0);
        assertEquals(AccountType.FIXED_DEPOSIT, a.getAccountType());
        assertEquals(AccountType.SAVINGS, factory.create(" savings ", "N2", "Asha", 30, 20000, "1234", 0).getAccountType());
    }

    @Test
    void rejectsUnknownOrMissingTypes() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> factory.create("gold", "N1", "Asha", 30, 1000, "1234", 0));
        assertTrue(e.getMessage().contains("Unknown account type"));
        assertThrows(IllegalArgumentException.class,
                () -> factory.create((AccountType) null, "N1", "Asha", 30, 1000, "1234", 0));
    }

    @Test
    void rejectsMissingRules() {
        assertThrows(IllegalArgumentException.class, () -> new AccountFactory(null));
    }

    @Test
    void createdAccountsUseTheInjectedRules() {
        AccountRules strict = new AccountRules() {
            public double getMinimumBalance(AccountType t, int y) { return 9999; }
            public double getInterestRate(AccountType t, int y) { return 1; }
            public String getFeatureName(AccountType t, int y) { return "Stub"; }
            public double getDailyTransferLimit(AccountType t, int y) { return 1; }
            public double getFeatureValue(AccountType t, int y, String k, double fallback) { return fallback; }
        };
        AccountFactory custom = new AccountFactory(strict);
        assertThrows(IllegalArgumentException.class,
                () -> custom.create(AccountType.SAVINGS, "N1", "Asha", 30, 1000, "1234", 0));
        IAccount ok = custom.create(AccountType.SAVINGS, "N1", "Asha", 30, 10000, "1234", 0);
        assertEquals(9999.0, ok.getMinimumBalance());
    }
}
