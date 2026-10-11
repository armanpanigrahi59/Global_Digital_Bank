package com.gdb.rules;

import com.gdb.domain.AccountType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Reads the rules for one product from a .properties file.
 *
 * Format (one file per product, named after {@link AccountType#getConfigKey()}):
 * <pre>
 *   savings.tenure.0.minBalance=500
 *   savings.tenure.0.interestRate=2.5
 *   savings.tenure.0.featureName=Basic Savings            (optional)
 *   savings.tenure.0.dailyTransferLimit=50000             (optional, default 50000)
 *   current.tenure.0.feature.overdraftLimit=1000          (optional extras, any number)
 * </pre>
 * The number after "tenure." is the lowest tenure in whole years the row applies to.
 * Tier 0 is mandatory so every tenure resolves to a rule.
 *
 * Lookup order: the external directory (if one is given and has the file), then
 * the copy packaged in the jar under /rules/.
 */
public final class PropertiesRulesLoader {
    static final double DEFAULT_DAILY_LIMIT = 50000.0;
    private static final String RESOURCE_DIR = "/rules/";

    private PropertiesRulesLoader() {
    }

    static String fileName(AccountType type) {
        return type.getConfigKey() + ".properties";
    }

    /** Modification time of the external file, or -1 if there is none (or no external directory). */
    static long lastModified(AccountType type, Path externalDir) {
        if (externalDir == null) {
            return -1L;
        }
        Path file = externalDir.resolve(fileName(type));
        try {
            return Files.exists(file) ? Files.getLastModifiedTime(file).toMillis() : -1L;
        } catch (IOException e) {
            return -1L;
        }
    }

    /**
     * @param externalDir directory that may override the packaged rules; null to use only the packaged ones
     * @throws IOException           if a file exists but cannot be read
     * @throws IllegalStateException if the content is invalid or no rules exist for the product
     */
    public static Map<Integer, AccountRule> load(AccountType type, Path externalDir) throws IOException {
        Properties props = new Properties();
        String source;
        Path external = externalDir == null ? null : externalDir.resolve(fileName(type));
        if (external != null && Files.exists(external)) {
            try (InputStream in = Files.newInputStream(external)) {
                props.load(in);
            }
            source = external.toString();
        } else {
            String resource = RESOURCE_DIR + fileName(type);
            try (InputStream in = PropertiesRulesLoader.class.getResourceAsStream(resource)) {
                if (in == null) {
                    throw new IllegalStateException("No rules found for " + type + " (missing " + resource + ")");
                }
                props.load(in);
            }
            source = "classpath:" + resource;
        }
        return parse(type, props, source);
    }

    static Map<Integer, AccountRule> parse(AccountType type, Properties props, String source) {
        String prefix = type.getConfigKey() + ".tenure.";
        Map<Integer, AccountRule> rules = new TreeMap<>();
        for (int tier : tierKeys(props, prefix, source)) {
            String base = prefix + tier + ".";
            Map<String, Object> extras = new LinkedHashMap<>();
            String extraPrefix = base + "feature.";
            for (String name : new TreeSet<>(props.stringPropertyNames())) {
                if (name.startsWith(extraPrefix)) {
                    extras.put(name.substring(extraPrefix.length()), parseValue(props.getProperty(name).trim()));
                }
            }
            rules.put(tier, new AccountRule(
                    number(props, base + "minBalance", source, null),
                    number(props, base + "interestRate", source, null),
                    props.getProperty(base + "featureName", type.getDisplayName() + " tier " + tier + "+"),
                    number(props, base + "dailyTransferLimit", source, DEFAULT_DAILY_LIMIT),
                    extras));
        }
        if (!rules.containsKey(0)) {
            throw new IllegalStateException(source + " must define tenure tier 0 (" + prefix + "0.*)");
        }
        return rules;
    }

    private static int[] tierKeys(Properties props, String prefix, String source) {
        TreeSet<Integer> keys = new TreeSet<>();
        for (String name : props.stringPropertyNames()) {
            if (name.startsWith(prefix)) {
                String rest = name.substring(prefix.length());
                int dot = rest.indexOf('.');
                if (dot > 0) {
                    try {
                        keys.add(Integer.parseInt(rest.substring(0, dot)));
                    } catch (NumberFormatException e) {
                        throw new IllegalStateException(source + ": bad tenure tier in key '" + name + "'");
                    }
                }
            }
        }
        return keys.stream().mapToInt(Integer::intValue).toArray();
    }

    private static double number(Properties props, String key, String source, Double fallback) {
        String raw = props.getProperty(key);
        if (raw == null) {
            if (fallback != null) {
                return fallback;
            }
            throw new IllegalStateException(source + ": missing required key '" + key + "'");
        }
        try {
            double value = Double.parseDouble(raw.trim());
            if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalStateException(source + ": key '" + key + "' is not a non-negative number: '" + raw + "'");
        }
    }

    private static Object parseValue(String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return raw;
        }
    }
}
