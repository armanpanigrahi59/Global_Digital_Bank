package com.gdb.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gdb.domain.AccountType;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountRulesEngineTest {

    private AccountRulesEngine engine;
    private Path dir;

    @BeforeEach
    void setUp() throws Exception {
        engine = AccountRulesEngine.getInstance();
        engine.useClasspathRules();
        dir = Files.createTempDirectory("gdb-rules");
    }

    @AfterEach
    void tearDown() throws Exception {
        engine.useClasspathRules(); // the engine is a singleton: leave it as we found it
        try (var files = Files.list(dir)) {
            for (Path p : (Iterable<Path>) files::iterator) {
                Files.delete(p);
            }
        }
        Files.delete(dir);
    }

    private void write(String file, String content) throws Exception {
        Files.write(dir.resolve(file), content.getBytes(StandardCharsets.UTF_8));
    }

    private void touchForward(String file) throws Exception {
        Path p = dir.resolve(file);
        Files.setLastModifiedTime(p, FileTime.fromMillis(Files.getLastModifiedTime(p).toMillis() + 5000));
    }

    private static final String SAVINGS_V2 = "savings.tenure.0.minBalance=700\nsavings.tenure.0.interestRate=9\n";

    @Test
    void isASingleton() {
        assertSame(AccountRulesEngine.getInstance(), AccountRulesEngine.getInstance());
    }

    @Test
    void packagedRulesCoverEveryProductAndTier() {
        double[][] expected = {
            // type ordinal, tenure, minBalance, rate
            {0, 0, 500, 2.5}, {0, 1, 1000, 3.5}, {0, 2, 1000, 3.5}, {0, 3, 2000, 4.5}, {0, 10, 2000, 4.5},
            {1, 0, 0, 0}, {1, 5, 0, 0},
            {2, 0, 5000, 5.0}, {2, 1, 5000, 6.5}, {2, 3, 10000, 7.0},
            {3, 0, 0, 3.0}, {3, 1, 0, 3.5}, {3, 3, 0, 4.0},
        };
        for (double[] row : expected) {
            AccountType type = AccountType.values()[(int) row[0]];
            int tenure = (int) row[1];
            assertEquals(row[2], engine.getMinimumBalance(type, tenure), type + "/" + tenure);
            assertEquals(row[3], engine.getInterestRate(type, tenure), type + "/" + tenure);
        }
        assertEquals("Basic Savings", engine.getFeatureName(AccountType.SAVINGS, 0));
        assertEquals(100000.0, engine.getDailyTransferLimit(AccountType.SAVINGS, 1));
        assertEquals(5000.0, engine.getFeatureValue(AccountType.CURRENT, 1, "overdraftLimit", -1));
        assertEquals(7.0, engine.getFeatureValue(AccountType.SAVINGS, 0, "nope", 7.0));
        assertEquals(200000.0, engine.getAdditionalFeature(AccountType.SALARY, 3, "monthlyCreditLimit"));
    }

    @Test
    void rejectsNegativeTenure() {
        assertThrows(IllegalArgumentException.class, () -> engine.getMinimumBalance(AccountType.SAVINGS, -1));
        assertThrows(IllegalArgumentException.class, () -> engine.getTenureBucketName(-1));
    }

    @Test
    void namesTheTenureBuckets() {
        assertTrue(engine.getTenureBucketName(0).startsWith("NEW"));
        assertTrue(engine.getTenureBucketName(2).startsWith("ESTABLISHED"));
        assertTrue(engine.getTenureBucketName(3).startsWith("LOYAL"));
    }

    @Test
    void externalFileOverridesAndOthersFallBackToPackaged() throws Exception {
        write("savings.properties", SAVINGS_V2);
        engine.useExternalDirectory(dir);
        assertEquals(700.0, engine.getMinimumBalance(AccountType.SAVINGS, 0));
        assertEquals(9.0, engine.getInterestRate(AccountType.SAVINGS, 5)); // single tier covers all tenures
        assertEquals(5.0, engine.getInterestRate(AccountType.FIXED_DEPOSIT, 0)); // packaged
        assertEquals(dir, engine.getExternalDirectory().get());
    }

    @Test
    void reloadPicksUpEditsAndDetectsChanges() throws Exception {
        write("savings.properties", SAVINGS_V2);
        engine.useExternalDirectory(dir);
        assertFalse(engine.reloadIfChanged());

        write("savings.properties", "savings.tenure.0.minBalance=800\nsavings.tenure.0.interestRate=1\n");
        touchForward("savings.properties");
        assertTrue(engine.reloadIfChanged());
        assertEquals(800.0, engine.getMinimumBalance(AccountType.SAVINGS, 0));
        assertFalse(engine.reloadIfChanged());

        write("savings.properties", SAVINGS_V2);
        touchForward("savings.properties");
        engine.reloadRules();
        assertEquals(700.0, engine.getMinimumBalance(AccountType.SAVINGS, 0));
        assertNotNull(engine.getLastLoadTime());
    }

    @Test
    void aBrokenEditKeepsTheOldRules() throws Exception {
        write("savings.properties", SAVINGS_V2);
        engine.useExternalDirectory(dir);
        write("savings.properties", "savings.tenure.0.minBalance=abc\nsavings.tenure.0.interestRate=1\n");
        touchForward("savings.properties");
        assertThrows(IllegalStateException.class, () -> engine.reloadIfChanged());
        assertEquals(700.0, engine.getMinimumBalance(AccountType.SAVINGS, 0));
    }

    @Test
    void anUnusableDirectoryIsNotAdopted() throws Exception {
        write("savings.properties", "savings.tenure.0.minBalance=-5\nsavings.tenure.0.interestRate=1\n");
        assertThrows(IllegalStateException.class, () -> engine.useExternalDirectory(dir));
        assertFalse(engine.getExternalDirectory().isPresent());
        assertEquals(500.0, engine.getMinimumBalance(AccountType.SAVINGS, 0));
    }

    @Test
    void printsEveryProduct() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        engine.printAllRules(new PrintStream(bytes));
        String text = bytes.toString();
        assertTrue(text.contains("Basic Savings") && text.contains("Premier Current"));
    }
}
