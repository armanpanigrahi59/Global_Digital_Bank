package com.gdb.domain;

/**
 * The business rules an account needs, looked up by product and tenure.
 *
 * The domain depends only on this interface. The rules package supplies the
 * real implementation (read from .properties files); tests can supply a
 * trivial one. "Tenure" is the whole number of years the customer has held the account.
 */
public interface AccountRules {
    double getMinimumBalance(AccountType type, int tenureYears);

    /** Annual interest rate in percent. */
    double getInterestRate(AccountType type, int tenureYears);

    /** Marketing name of the product tier, e.g. "Premium Savings". */
    String getFeatureName(AccountType type, int tenureYears);

    double getDailyTransferLimit(AccountType type, int tenureYears);

    /** A product-specific numeric extra such as "overdraftLimit"; the fallback if it is not defined. */
    double getFeatureValue(AccountType type, int tenureYears, String featureKey, double fallback);
}
