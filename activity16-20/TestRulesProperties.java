import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.stream.Stream;

/**
 * Activity 14 - Tests the properties-backed rules engine: loading, defaults,
 * hot reload, failure safety, and that accounts really use the loaded rules.
 * Run from the activity14-15 folder so ./config resolves.
 */
public class TestRulesProperties {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        AccountRulesEngine engine = AccountRulesEngine.getInstance();

        System.out.println("=== Rules loaded from ./config ===");
        check("Engine is loaded with 4 account types", engine.isLoaded() && engine.getAccountTypeCount() == 4);
        check("Load time recorded", engine.getLastLoadTime() != null);
        check("SAVINGS 0y min 500", engine.getMinimumBalance("SAVINGS", 0) == 500);
        check("SAVINGS 2y rate 3.5", engine.getInterestRate("SAVINGS", 2) == 3.5);
        check("SAVINGS 7y feature Premium Savings", engine.getFeatureName("SAVINGS", 7).equals("Premium Savings"));
        check("FIXED alias resolves", engine.getInterestRate("FIXED", 3) == 7.0);
        check("FIXED_DEPOSIT 3y min 10000", engine.getMinimumBalance("FIXED_DEPOSIT", 3) == 10000);
        check("CURRENT 1y overdraft extra 5000", engine.getAdditionalFeatureAsDouble("CURRENT", 1, "overdraftLimit", -1) == 5000);
        check("SALARY 3y monthlyCreditLimit 200000", engine.getAdditionalFeatureAsDouble("SALARY", 3, "monthlyCreditLimit", -1) == 200000);
        check("Missing extra returns null", engine.getAdditionalFeature("SAVINGS", 0, "overdraftLimit") == null);
        check("SAVINGS 0y daily limit 50000", engine.getDailyTransferLimit("SAVINGS", 0) == 50000);
        check("SAVINGS 3y daily limit 200000", engine.getDailyTransferLimit("SAVINGS", 3) == 200000);
        check("hasAccountType true / false", engine.hasAccountType("salary") && !engine.hasAccountType("GOLD"));
        check("Bucket name", engine.getTenureBucketName(5).startsWith("LOYAL"));

        System.out.println("\n=== Accounts pick up engine rules ===");
        IAccount s0 = AccountFactory.createAccount("SAVINGS", "S0", "Ann", 30, 600, "1", 0);
        IAccount s3 = AccountFactory.createAccount("SAVINGS", "S3", "Ben", 30, 3000, "1", 3);
        check("New savings min 500, rate 2.5", s0.getMinimumBalance() == 500 && s0.getInterestRate() == 2.5);
        check("3-year savings min 2000, rate 4.5", s3.getMinimumBalance() == 2000 && s3.getInterestRate() == 4.5);
        try {
            AccountFactory.createAccount("SAVINGS", "S9", "Cy", 30, 1500, "1", 3);
            check("1500 opening on 3-year savings should be rejected (min 2000)", false);
        } catch (IllegalArgumentException e) {
            check("Opening balance validated against tenure-specific minimum", true);
        }
        CurrentAccount ca = new CurrentAccount("C1", "Dee", 30, 100, "1", 3);
        check("3-year current overdraft is 10000", ca.getOverdraftLimit() == 10000);
        SalaryAccount sal = new SalaryAccount("P1", "Eve", 30, 0, "1", 1);
        check("1-year salary monthly credit limit 100000", sal.getMonthlyCreditLimit() == 100000);

        System.out.println("\n=== Hot reload (works on a temp copy of the config) ===");
        Path temp = Files.createTempDirectory("gdb-rules");
        try {
            try (Stream<Path> files = Files.list(Path.of("config"))) {
                for (Path p : (Iterable<Path>) files::iterator) {
                    Files.copy(p, temp.resolve(p.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            engine.setConfigPath(temp.toString());
            check("Engine now reads from the temp directory", engine.getConfigPath().equals(temp.toString()));
            check("Unchanged files: reloadIfChanged is false", !engine.reloadIfChanged());

            Path savings = temp.resolve("savings.properties");
            String edited = Files.readString(savings).replace("savings.tenure.0.interestRate=2.5", "savings.tenure.0.interestRate=9.9");
            Files.writeString(savings, edited);
            Files.setLastModifiedTime(savings, FileTime.fromMillis(System.currentTimeMillis() + 5000));
            check("Edit not visible until reload", engine.getInterestRate("SAVINGS", 0) == 2.5);
            check("reloadIfChanged detects the edit", engine.reloadIfChanged());
            check("New rate 9.9 is live", engine.getInterestRate("SAVINGS", 0) == 9.9);
            check("Existing account sees the new rate immediately", s0.getInterestRate() == 9.9);
            check("Other rules unaffected", engine.getInterestRate("SAVINGS", 1) == 3.5);

            engine.reloadRules();
            check("Explicit reloadRules keeps the edited value", engine.getInterestRate("SAVINGS", 0) == 9.9);

            System.out.println("\n=== A bad edit is rejected and old rules stay ===");
            Files.writeString(savings, edited.replace("savings.tenure.0.minBalance=500", "savings.tenure.0.minBalance=lots"));
            Files.setLastModifiedTime(savings, FileTime.fromMillis(System.currentTimeMillis() + 10000));
            try {
                engine.reloadIfChanged();
                check("Non-numeric value should fail the reload", false);
            } catch (IllegalStateException e) {
                check("Reload failed: " + e.getMessage(), e.getMessage().contains("not a number"));
            }
            check("Previous rules still active after failed reload", engine.getMinimumBalance("SAVINGS", 0) == 500
                    && engine.getInterestRate("SAVINGS", 0) == 9.9);

            Files.writeString(savings, edited.replace("savings.tenure.0.minBalance=500\n", "")
                    .replace("savings.tenure.0.minBalance=500\r\n", ""));
            Files.setLastModifiedTime(savings, FileTime.fromMillis(System.currentTimeMillis() + 15000));
            try {
                engine.reloadIfChanged();
                check("Missing required key should fail the reload", false);
            } catch (IllegalStateException e) {
                check("Reload failed: " + e.getMessage(), e.getMessage().contains("missing required key"));
            }

            Files.writeString(savings, "savings.tenure.1.minBalance=1\nsavings.tenure.1.interestRate=1\n");
            Files.setLastModifiedTime(savings, FileTime.fromMillis(System.currentTimeMillis() + 20000));
            try {
                engine.reloadIfChanged();
                check("File without bucket 0 should fail the reload", false);
            } catch (IllegalStateException e) {
                check("Reload failed: " + e.getMessage(), e.getMessage().contains("bucket 0"));
            }

            System.out.println("\n=== Missing file falls back to built-in defaults ===");
            Files.delete(savings);
            check("Deleting a file counts as a change", engine.reloadIfChanged());
            check("Savings falls back to defaults (2.5 at 0y)", engine.getInterestRate("SAVINGS", 0) == 2.5);
            check("Other types still read from files", engine.getInterestRate("SALARY", 1) == 3.5);
        } finally {
            engine.setConfigPath("config"); // restore the real config
            try (Stream<Path> files = Files.list(temp)) {
                files.forEach(p -> p.toFile().delete());
            }
            Files.deleteIfExists(temp);
        }

        System.out.println("\n=== Bad config path ===");
        String before = engine.getConfigPath();
        Path emptyDir = Files.createTempDirectory("gdb-empty");
        try {
            engine.setConfigPath(emptyDir.toString());
            check("Empty directory -> all defaults", engine.getInterestRate("FIXED_DEPOSIT", 0) == 5.0);
        } finally {
            engine.setConfigPath(before);
            Files.deleteIfExists(emptyDir);
        }
        check("Config path restored", engine.getConfigPath().equals("config"));

        System.out.println("\nResult: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(String label, boolean ok) {
        if (ok) passed++; else failed++;
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + label);
    }
}
