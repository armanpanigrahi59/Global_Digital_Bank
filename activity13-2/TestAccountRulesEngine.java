/**
 * Activity 13 - Verifies the rules engine. The identical test runs against
 * the if-else version (13.1) and the map-based version (13.2): passing on
 * both proves the refactor preserved behaviour.
 */
public class TestAccountRulesEngine {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        AccountRulesEngine engine = AccountRulesEngine.getInstance();

        System.out.println("=== Singleton ===");
        check("getInstance returns the same object", engine == AccountRulesEngine.getInstance());

        System.out.println("\n=== Tenure buckets ===");
        check("0 years -> NEW", engine.getTenureBucketName(0).startsWith("NEW"));
        check("1 year -> ESTABLISHED", engine.getTenureBucketName(1).startsWith("ESTABLISHED"));
        check("2 years -> ESTABLISHED", engine.getTenureBucketName(2).startsWith("ESTABLISHED"));
        check("3 years -> LOYAL", engine.getTenureBucketName(3).startsWith("LOYAL"));
        check("20 years -> LOYAL", engine.getTenureBucketName(20).startsWith("LOYAL"));

        System.out.println("\n=== Rules per type and tenure: {type, tenure, minBalance, rate, feature} ===");
        Object[][] table = {
                {"SAVINGS", 0, 500.0, 2.5, "Basic Savings"},
                {"SAVINGS", 1, 1000.0, 3.5, "Regular Savings"},
                {"SAVINGS", 2, 1000.0, 3.5, "Regular Savings"},
                {"SAVINGS", 3, 2000.0, 4.5, "Premium Savings"},
                {"SAVINGS", 10, 2000.0, 4.5, "Premium Savings"},
                {"CURRENT", 0, 0.0, 0.0, "Basic Current"},
                {"CURRENT", 1, 0.0, 0.0, "Business Current"},
                {"CURRENT", 5, 0.0, 0.0, "Premier Current"},
                {"FIXED_DEPOSIT", 0, 5000.0, 5.0, "Short-Term FD"},
                {"FIXED_DEPOSIT", 2, 5000.0, 6.5, "Standard FD"},
                {"FIXED_DEPOSIT", 3, 10000.0, 7.0, "Long-Term FD"},
                {"SALARY", 0, 0.0, 3.0, "Salary Basic"},
                {"SALARY", 1, 0.0, 3.5, "Salary Plus"},
                {"SALARY", 4, 0.0, 4.0, "Salary Elite"},
        };
        for (Object[] row : table) {
            String type = (String) row[0];
            int tenure = (Integer) row[1];
            String label = type + " @ " + tenure + "y";
            check(label + " min balance " + row[2], engine.getMinimumBalance(type, tenure) == (Double) row[2]);
            check(label + " rate " + row[3], engine.getInterestRate(type, tenure) == (Double) row[3]);
            check(label + " feature " + row[4], engine.getFeatureName(type, tenure).equals(row[4]));
        }

        System.out.println("\n=== Type name handling ===");
        check("FIXED is an alias of FIXED_DEPOSIT", engine.getInterestRate("FIXED", 0) == 5.0);
        check("Type is case-insensitive", engine.getInterestRate("savings", 0) == 2.5);
        check("Type is trimmed", engine.getInterestRate("  Salary ", 0) == 3.0);

        System.out.println("\n=== Invalid input ===");
        expectError("Unknown type", () -> engine.getInterestRate("PLATINUM", 1));
        expectError("Null type", () -> engine.getMinimumBalance(null, 1));
        expectError("Negative tenure (rate)", () -> engine.getInterestRate("SAVINGS", -1));
        expectError("Negative tenure (bucket)", () -> engine.getTenureBucketName(-1));

        System.out.println("\nResult: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void expectError(String label, Runnable r) {
        try {
            r.run();
            check(label + " should be rejected", false);
        } catch (IllegalArgumentException e) {
            check(label + " rejected: " + e.getMessage(), true);
        }
    }

    private static void check(String label, boolean ok) {
        if (ok) passed++; else failed++;
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + label);
    }
}
