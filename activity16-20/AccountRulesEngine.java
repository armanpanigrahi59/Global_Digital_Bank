import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Activity 14 - Rules engine backed by externalized .properties files.
 *
 * Rules live in one file per account type (savings.properties, ...), found
 * in the config directory (default: ./config). Bank staff can change a rate
 * or limit by editing a file; reloadRules() / reloadIfChanged() pick it up
 * without restarting the application ("hot reload").
 *
 * A failed reload (bad number, unreadable file) throws and leaves the rules
 * that were already loaded untouched.
 */
public class AccountRulesEngine {
    public static final String DEFAULT_CONFIG_PATH = "config";
    static final List<String> ACCOUNT_TYPES = List.of("SAVINGS", "CURRENT", "FIXED_DEPOSIT", "SALARY");

    /** One row of the rules table for a type and tenure bucket. */
    public static class Rule {
        private final double minBalance;
        private final double interestRate;
        private final String featureName;
        private final double dailyTransferLimit;
        private final Map<String, Object> additionalFeatures = new HashMap<>();

        public Rule(double minBalance, double interestRate, String featureName, double dailyTransferLimit) {
            this.minBalance = minBalance;
            this.interestRate = interestRate;
            this.featureName = featureName;
            this.dailyTransferLimit = dailyTransferLimit;
        }

        public double getMinBalance() { return minBalance; }
        public double getInterestRate() { return interestRate; }
        public String getFeatureName() { return featureName; }
        public double getDailyTransferLimit() { return dailyTransferLimit; }
        public Map<String, Object> getAdditionalFeatures() { return additionalFeatures; }

        public void addFeature(String key, Object value) {
            additionalFeatures.put(key, value);
        }

        @Override
        public String toString() {
            return "Rule[min=" + minBalance + ", rate=" + interestRate + ", feature=" + featureName
                    + ", dailyLimit=" + dailyTransferLimit + ", extras=" + additionalFeatures + "]";
        }
    }

    private static AccountRulesEngine instance;

    private Map<String, TreeMap<Integer, Rule>> rulesMap = new HashMap<>();
    private final Map<String, Long> fileTimestamps = new HashMap<>();
    private String configPath = DEFAULT_CONFIG_PATH;
    private boolean loaded = false;
    private LocalDateTime lastLoadTime;

    private AccountRulesEngine() {
        loadAllRules();
    }

    public static synchronized AccountRulesEngine getInstance() {
        if (instance == null) {
            instance = new AccountRulesEngine();
        }
        return instance;
    }

    // ---- Loading ----

    /** Points the engine at a different config directory and loads it. */
    public synchronized void setConfigPath(String configPath) {
        String previous = this.configPath;
        this.configPath = configPath;
        try {
            loadAllRules();
        } catch (RuntimeException e) {
            this.configPath = previous; // keep a consistent engine on failure
            throw e;
        }
    }

    public synchronized String getConfigPath() {
        return configPath;
    }

    public synchronized void loadAllRules() {
        Map<String, TreeMap<Integer, Rule>> fresh = new HashMap<>();
        Map<String, Long> stamps = new HashMap<>();
        try {
            for (String type : ACCOUNT_TYPES) {
                fresh.put(type, new TreeMap<>(AccountRulesPropertiesLoader.loadRules(type, configPath)));
                stamps.put(type, AccountRulesPropertiesLoader.lastModified(type, configPath));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read rules from '" + configPath + "': " + e.getMessage(), e);
        }
        rulesMap = fresh; // swap only after everything parsed
        fileTimestamps.clear();
        fileTimestamps.putAll(stamps);
        loaded = true;
        lastLoadTime = LocalDateTime.now();
    }

    /** Re-reads every rules file now. */
    public synchronized void reloadRules() {
        loadAllRules();
    }

    /** Reloads only if a rules file was added, removed or modified. @return true if a reload happened */
    public synchronized boolean reloadIfChanged() {
        for (String type : ACCOUNT_TYPES) {
            long now = AccountRulesPropertiesLoader.lastModified(type, configPath);
            if (now != fileTimestamps.getOrDefault(type, Long.MIN_VALUE)) {
                loadAllRules();
                return true;
            }
        }
        return false;
    }

    // ---- Lookups ----

    public synchronized double getMinimumBalance(String type, int tenure) {
        return rule(type, tenure).getMinBalance();
    }

    public synchronized double getInterestRate(String type, int tenure) {
        return rule(type, tenure).getInterestRate();
    }

    public synchronized String getFeatureName(String type, int tenure) {
        return rule(type, tenure).getFeatureName();
    }

    public synchronized double getDailyTransferLimit(String type, int tenure) {
        return rule(type, tenure).getDailyTransferLimit();
    }

    /** Type-specific extras such as overdraftLimit or monthlyCreditLimit; null if absent. */
    public synchronized Object getAdditionalFeature(String type, int tenure, String key) {
        return rule(type, tenure).getAdditionalFeatures().get(key);
    }

    public synchronized double getAdditionalFeatureAsDouble(String type, int tenure, String key, double fallback) {
        Object v = getAdditionalFeature(type, tenure, key);
        return v instanceof Number ? ((Number) v).doubleValue() : fallback;
    }

    public String getTenureBucketName(int tenure) {
        checkTenure(tenure);
        if (tenure < 1) return "NEW (under 1 year)";
        if (tenure < 3) return "ESTABLISHED (1 to 2 years)";
        return "LOYAL (3+ years)";
    }

    public synchronized boolean hasAccountType(String type) {
        return type != null && rulesMap.containsKey(normalize(type));
    }

    public synchronized boolean isLoaded() { return loaded; }

    public synchronized LocalDateTime getLastLoadTime() { return lastLoadTime; }

    public synchronized int getAccountTypeCount() { return rulesMap.size(); }

    public synchronized void printAllRules() {
        for (String type : ACCOUNT_TYPES) {
            System.out.println(type + ":");
            rulesMap.get(type).forEach((bucket, rule) -> System.out.println("  from " + bucket + "y -> " + rule));
        }
    }

    // ---- Internals ----

    /** Finds the rule whose bucket is the highest one not above the tenure. */
    private Rule rule(String type, int tenure) {
        checkTenure(tenure);
        if (type == null) {
            throw new IllegalArgumentException("Account type must not be null");
        }
        TreeMap<Integer, Rule> buckets = rulesMap.get(normalize(type));
        if (buckets == null) {
            throw new IllegalArgumentException("Unknown account type: " + type);
        }
        return buckets.floorEntry(tenure).getValue(); // loader guarantees bucket 0 exists
    }

    static String normalize(String type) {
        String t = type.trim().toUpperCase();
        return t.equals("FIXED") ? "FIXED_DEPOSIT" : t;
    }

    private static void checkTenure(int tenure) {
        if (tenure < 0) {
            throw new IllegalArgumentException("Tenure must not be negative: " + tenure);
        }
    }
}
