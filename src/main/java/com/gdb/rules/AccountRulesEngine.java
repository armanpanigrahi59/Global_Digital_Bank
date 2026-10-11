package com.gdb.rules;

import com.gdb.domain.AccountRules;
import com.gdb.domain.AccountType;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Singleton that holds the bank's rules and answers lookups by product and tenure.
 *
 * Rules ship inside the jar (src/main/resources/rules/*.properties). To change
 * them without rebuilding, point the engine at a directory containing edited
 * copies, either with the system property {@value #RULES_DIR_PROPERTY} or
 * {@link #useExternalDirectory(Path)}, then call {@link #reloadRules()} or
 * {@link #reloadIfChanged()} after editing ("hot reload"). Files missing from
 * the external directory fall back to the packaged ones.
 *
 * A failed reload (bad number, unreadable file) throws and leaves the rules
 * that were already loaded untouched.
 */
public final class AccountRulesEngine implements AccountRules {
    /** System property naming a directory of rules files that override the packaged ones. */
    public static final String RULES_DIR_PROPERTY = "gdb.rules.dir";

    private static AccountRulesEngine instance;

    private Map<AccountType, NavigableMap<Integer, AccountRule>> rulesByType = new EnumMap<>(AccountType.class);
    private final Map<AccountType, Long> fileTimestamps = new HashMap<>();
    private Path externalDir;
    private LocalDateTime lastLoadTime;

    private AccountRulesEngine() {
        String configured = System.getProperty(RULES_DIR_PROPERTY);
        this.externalDir = (configured == null || configured.isBlank()) ? null : Paths.get(configured);
        loadAllRules();
    }

    public static synchronized AccountRulesEngine getInstance() {
        if (instance == null) {
            instance = new AccountRulesEngine();
        }
        return instance;
    }

    // ---- Where the rules come from ----

    /** Reads rules from this directory (falling back to packaged ones per file) and loads them now. */
    public synchronized void useExternalDirectory(Path directory) {
        Path previous = this.externalDir;
        this.externalDir = directory;
        try {
            loadAllRules();
        } catch (RuntimeException e) {
            this.externalDir = previous; // stay consistent if the new directory is unusable
            throw e;
        }
    }

    /** Goes back to the rules packaged in the jar. */
    public synchronized void useClasspathRules() {
        useExternalDirectory(null);
    }

    public synchronized Optional<Path> getExternalDirectory() {
        return Optional.ofNullable(externalDir);
    }

    // ---- Loading ----

    /** Re-reads every rules file now. */
    public synchronized void reloadRules() {
        loadAllRules();
    }

    /** Reloads only if an external rules file was added, edited or removed. @return true if it reloaded */
    public synchronized boolean reloadIfChanged() {
        for (AccountType type : AccountType.values()) {
            long now = PropertiesRulesLoader.lastModified(type, externalDir);
            if (now != fileTimestamps.getOrDefault(type, -1L)) {
                loadAllRules();
                return true;
            }
        }
        return false;
    }

    private void loadAllRules() {
        Map<AccountType, NavigableMap<Integer, AccountRule>> fresh = new EnumMap<>(AccountType.class);
        Map<AccountType, Long> stamps = new HashMap<>();
        try {
            for (AccountType type : AccountType.values()) {
                fresh.put(type, new TreeMap<>(PropertiesRulesLoader.load(type, externalDir)));
                stamps.put(type, PropertiesRulesLoader.lastModified(type, externalDir));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read account rules: " + e.getMessage(), e);
        }
        rulesByType = fresh; // swap only after everything parsed
        fileTimestamps.clear();
        fileTimestamps.putAll(stamps);
        lastLoadTime = LocalDateTime.now();
    }

    public synchronized LocalDateTime getLastLoadTime() {
        return lastLoadTime;
    }

    // ---- AccountRules ----

    @Override
    public synchronized double getMinimumBalance(AccountType type, int tenureYears) {
        return rule(type, tenureYears).getMinBalance();
    }

    @Override
    public synchronized double getInterestRate(AccountType type, int tenureYears) {
        return rule(type, tenureYears).getInterestRate();
    }

    @Override
    public synchronized String getFeatureName(AccountType type, int tenureYears) {
        return rule(type, tenureYears).getFeatureName();
    }

    @Override
    public synchronized double getDailyTransferLimit(AccountType type, int tenureYears) {
        return rule(type, tenureYears).getDailyTransferLimit();
    }

    @Override
    public synchronized double getFeatureValue(AccountType type, int tenureYears, String featureKey, double fallback) {
        Object value = rule(type, tenureYears).getAdditionalFeatures().get(featureKey);
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    // ---- Extras ----

    /** Any extra as stored (Double or String); null if not defined. */
    public synchronized Object getAdditionalFeature(AccountType type, int tenureYears, String featureKey) {
        return rule(type, tenureYears).getAdditionalFeatures().get(featureKey);
    }

    public String getTenureBucketName(int tenureYears) {
        checkTenure(tenureYears);
        if (tenureYears < 1) {
            return "NEW (under 1 year)";
        }
        if (tenureYears < 3) {
            return "ESTABLISHED (1 to 2 years)";
        }
        return "LOYAL (3+ years)";
    }

    public synchronized void printAllRules(PrintStream out) {
        for (AccountType type : AccountType.values()) {
            out.println(type.getDisplayName() + ":");
            rulesByType.get(type).forEach((tier, rule) -> out.println("  from " + tier + " year(s): " + rule));
        }
    }

    // ---- Internals ----

    /** The rule for the highest tier not above the tenure (tier 0 always exists). */
    private AccountRule rule(AccountType type, int tenureYears) {
        if (type == null) {
            throw new IllegalArgumentException("Account type must not be null");
        }
        checkTenure(tenureYears);
        return rulesByType.get(type).floorEntry(tenureYears).getValue();
    }

    private static void checkTenure(int tenureYears) {
        if (tenureYears < 0) {
            throw new IllegalArgumentException("Tenure must not be negative: " + tenureYears);
        }
    }
}
