package com.gdb.rules;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One row of the rules table: everything that applies to a product from a
 * given tenure upwards. Immutable.
 */
public final class AccountRule {
    private final double minBalance;
    private final double interestRate;
    private final String featureName;
    private final double dailyTransferLimit;
    private final Map<String, Object> additionalFeatures;

    public AccountRule(double minBalance, double interestRate, String featureName,
                       double dailyTransferLimit, Map<String, Object> additionalFeatures) {
        this.minBalance = minBalance;
        this.interestRate = interestRate;
        this.featureName = featureName;
        this.dailyTransferLimit = dailyTransferLimit;
        this.additionalFeatures = Collections.unmodifiableMap(new LinkedHashMap<>(additionalFeatures));
    }

    public double getMinBalance() {
        return minBalance;
    }

    public double getInterestRate() {
        return interestRate;
    }

    public String getFeatureName() {
        return featureName;
    }

    public double getDailyTransferLimit() {
        return dailyTransferLimit;
    }

    /** Product-specific extras (e.g. overdraftLimit); values are Double for numbers, String otherwise. */
    public Map<String, Object> getAdditionalFeatures() {
        return additionalFeatures;
    }

    @Override
    public String toString() {
        return "min=" + minBalance + ", rate=" + interestRate + "%, feature=\"" + featureName
                + "\", dailyLimit=" + dailyTransferLimit
                + (additionalFeatures.isEmpty() ? "" : ", extras=" + additionalFeatures);
    }
}
