import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Activity 14 - Reads account rules from a .properties file.
 *
 * File layout (one file per type, e.g. savings.properties):
 *   savings.tenure.0.minBalance=500
 *   savings.tenure.0.interestRate=2.5
 *   savings.tenure.0.featureName=Basic Savings          (optional)
 *   savings.tenure.0.dailyTransferLimit=50000           (optional, default 50000)
 *   savings.tenure.0.feature.overdraftLimit=1000        (optional extras, any number)
 * The number after "tenure." is the lowest tenure (in years) the row applies to.
 * Bucket 0 is mandatory so every tenure resolves to a rule.
 *
 * If a type's file does not exist, built-in defaults are used, so the system
 * still starts with no config directory at all.
 */
public final class AccountRulesPropertiesLoader {
    static final double DEFAULT_DAILY_LIMIT = 50000.0;

    private AccountRulesPropertiesLoader() {
    }

    /** "FIXED_DEPOSIT" -> "fixeddeposit" (used for both the file name and the key prefix). */
    static String keyFor(String accountType) {
        return AccountRulesEngine.normalize(accountType).toLowerCase().replace("_", "");
    }

    static Path fileFor(String accountType, String configPath) {
        return Paths.get(configPath, keyFor(accountType) + ".properties");
    }

    /** File modification time in millis, or -1 if the file does not exist. */
    static long lastModified(String accountType, String configPath) {
        Path file = fileFor(accountType, configPath);
        try {
            return Files.exists(file) ? Files.getLastModifiedTime(file).toMillis() : -1L;
        } catch (IOException e) {
            return -1L;
        }
    }

    public static Map<Integer, AccountRulesEngine.Rule> loadRules(String accountType, String configPath)
            throws IOException {
        Path file = fileFor(accountType, configPath);
        if (!Files.exists(file)) {
            return getDefaultRules(accountType);
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        }

        String prefix = keyFor(accountType) + ".tenure.";
        Map<Integer, AccountRulesEngine.Rule> rules = new TreeMap<>();
        for (int bucket : getBucketKeys(props, prefix, file)) {
            String base = prefix + bucket + ".";
            AccountRulesEngine.Rule rule = new AccountRulesEngine.Rule(
                    number(props, base + "minBalance", file, null),
                    number(props, base + "interestRate", file, null),
                    props.getProperty(base + "featureName", accountType + " tenure " + bucket + "+"),
                    number(props, base + "dailyTransferLimit", file, DEFAULT_DAILY_LIMIT));
            String extraPrefix = base + "feature.";
            for (String name : props.stringPropertyNames()) {
                if (name.startsWith(extraPrefix)) {
                    rule.addFeature(name.substring(extraPrefix.length()), parseValue(props.getProperty(name).trim()));
                }
            }
            rules.put(bucket, rule);
        }
        if (!rules.containsKey(0)) {
            throw new IllegalStateException(file + " must define tenure bucket 0 (" + prefix + "0.*)");
        }
        return rules;
    }

    /** The tenure buckets a file defines, in ascending order. */
    static int[] getBucketKeys(Properties props, String prefix, Path file) {
        TreeSet<Integer> keys = new TreeSet<>();
        for (String name : props.stringPropertyNames()) {
            if (name.startsWith(prefix)) {
                String rest = name.substring(prefix.length());
                int dot = rest.indexOf('.');
                if (dot > 0) {
                    try {
                        keys.add(Integer.parseInt(rest.substring(0, dot)));
                    } catch (NumberFormatException e) {
                        throw new IllegalStateException(file + ": bad tenure bucket in key '" + name + "'");
                    }
                }
            }
        }
        return keys.stream().mapToInt(Integer::intValue).toArray();
    }

    private static double number(Properties props, String key, Path file, Double fallback) {
        String raw = props.getProperty(key);
        if (raw == null) {
            if (fallback != null) {
                return fallback;
            }
            throw new IllegalStateException(file + ": missing required key '" + key + "'");
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException(file + ": key '" + key + "' is not a number: '" + raw + "'");
        }
    }

    private static Object parseValue(String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    // ---- Built-in defaults (identical to the shipped config files) ----

    public static Map<Integer, AccountRulesEngine.Rule> getDefaultRules(String accountType) {
        Map<Integer, AccountRulesEngine.Rule> r = new TreeMap<>();
        switch (AccountRulesEngine.normalize(accountType)) {
            case "SAVINGS" -> {
                r.put(0, new AccountRulesEngine.Rule(500, 2.5, "Basic Savings", 50000));
                r.put(1, new AccountRulesEngine.Rule(1000, 3.5, "Regular Savings", 100000));
                r.put(3, new AccountRulesEngine.Rule(2000, 4.5, "Premium Savings", 200000));
            }
            case "CURRENT" -> {
                r.put(0, withExtra(new AccountRulesEngine.Rule(0, 0.0, "Basic Current", 100000), "overdraftLimit", 1000));
                r.put(1, withExtra(new AccountRulesEngine.Rule(0, 0.0, "Business Current", 200000), "overdraftLimit", 5000));
                r.put(3, withExtra(new AccountRulesEngine.Rule(0, 0.0, "Premier Current", 500000), "overdraftLimit", 10000));
            }
            case "FIXED_DEPOSIT" -> {
                r.put(0, new AccountRulesEngine.Rule(5000, 5.0, "Short-Term FD", 25000));
                r.put(1, new AccountRulesEngine.Rule(5000, 6.5, "Standard FD", 50000));
                r.put(3, new AccountRulesEngine.Rule(10000, 7.0, "Long-Term FD", 100000));
            }
            case "SALARY" -> {
                r.put(0, withExtra(new AccountRulesEngine.Rule(0, 3.0, "Salary Basic", 100000), "monthlyCreditLimit", 50000));
                r.put(1, withExtra(new AccountRulesEngine.Rule(0, 3.5, "Salary Plus", 300000), "monthlyCreditLimit", 100000));
                r.put(3, withExtra(new AccountRulesEngine.Rule(0, 4.0, "Salary Elite", 500000), "monthlyCreditLimit", 200000));
            }
            default -> throw new IllegalArgumentException("Unknown account type: " + accountType);
        }
        return r;
    }

    private static AccountRulesEngine.Rule withExtra(AccountRulesEngine.Rule rule, String key, double value) {
        rule.addFeature(key, value);
        return rule;
    }
}
