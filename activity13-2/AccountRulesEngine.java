import java.util.HashMap;
import java.util.Map;

/**
 * Activity 13.2 - Rules engine, version 2: nested Map lookup.
 *
 * The same rules as 13.1, but stored as data in
 * Map<accountType, Map<tenureBucket, Rule>>. A lookup is two hash gets
 * (O(1)); adding a type or a bucket is one line in loadRules() and the
 * public methods never change.
 */
public class AccountRulesEngine {
    private static AccountRulesEngine instance;

    /** One row of the rules table. */
    static class Rule {
        final double interestRate;
        final double minBalance;
        final String featureName;

        Rule(double interestRate, double minBalance, String featureName) {
            this.interestRate = interestRate;
            this.minBalance = minBalance;
            this.featureName = featureName;
        }
    }

    private final Map<String, Map<Integer, Rule>> rules = new HashMap<>();

    private AccountRulesEngine() {
        loadRules();
    }

    public static synchronized AccountRulesEngine getInstance() {
        if (instance == null) {
            instance = new AccountRulesEngine();
        }
        return instance;
    }

    private void loadRules() {
        add("SAVINGS", 0, 2.5, 500, "Basic Savings");
        add("SAVINGS", 1, 3.5, 1000, "Regular Savings");
        add("SAVINGS", 3, 4.5, 2000, "Premium Savings");

        add("CURRENT", 0, 0.0, 0, "Basic Current");
        add("CURRENT", 1, 0.0, 0, "Business Current");
        add("CURRENT", 3, 0.0, 0, "Premier Current");

        add("FIXED_DEPOSIT", 0, 5.0, 5000, "Short-Term FD");
        add("FIXED_DEPOSIT", 1, 6.5, 5000, "Standard FD");
        add("FIXED_DEPOSIT", 3, 7.0, 10000, "Long-Term FD");

        add("SALARY", 0, 3.0, 0, "Salary Basic");
        add("SALARY", 1, 3.5, 0, "Salary Plus");
        add("SALARY", 3, 4.0, 0, "Salary Elite");
    }

    private void add(String type, int bucket, double rate, double minBalance, String feature) {
        rules.computeIfAbsent(type, k -> new HashMap<>()).put(bucket, new Rule(rate, minBalance, feature));
    }

    /** Maps a tenure in years to the bucket key used in the rules table. */
    private static int bucketFor(int tenure) {
        if (tenure < 0) {
            throw new IllegalArgumentException("Tenure must not be negative: " + tenure);
        }
        if (tenure >= 3) return 3;
        if (tenure >= 1) return 1;
        return 0;
    }

    private Rule rule(String type, int tenure) {
        if (type == null) {
            throw new IllegalArgumentException("Account type must not be null");
        }
        String t = type.trim().toUpperCase();
        if (t.equals("FIXED")) {
            t = "FIXED_DEPOSIT";
        }
        Map<Integer, Rule> byBucket = rules.get(t);
        if (byBucket == null) {
            throw new IllegalArgumentException("Unknown account type: " + type);
        }
        return byBucket.get(bucketFor(tenure));
    }

    public String getTenureBucketName(int tenure) {
        switch (bucketFor(tenure)) {
            case 0: return "NEW (under 1 year)";
            case 1: return "ESTABLISHED (1 to 2 years)";
            default: return "LOYAL (3+ years)";
        }
    }

    public double getMinimumBalance(String type, int tenure) {
        return rule(type, tenure).minBalance;
    }

    public double getInterestRate(String type, int tenure) {
        return rule(type, tenure).interestRate;
    }

    public String getFeatureName(String type, int tenure) {
        return rule(type, tenure).featureName;
    }
}
