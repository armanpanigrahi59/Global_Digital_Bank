/**
 * Activity 13.1 - Rules engine, version 1: plain if-else chains.
 *
 * Rules depend on the account type and on tenure (whole years the customer
 * has held the account). Tenure falls into one of three buckets:
 *   0 -> under 1 year, 1 -> 1 to under 3 years, 3 -> 3 years or more.
 * Every rule lookup repeats the same type/bucket branching, which is what
 * Activity 13.2 refactors away.
 */
public class AccountRulesEngine {
    private static AccountRulesEngine instance;

    private AccountRulesEngine() {
    }

    public static synchronized AccountRulesEngine getInstance() {
        if (instance == null) {
            instance = new AccountRulesEngine();
        }
        return instance;
    }

    public String getTenureBucketName(int tenure) {
        checkTenure(tenure);
        if (tenure < 1) {
            return "NEW (under 1 year)";
        } else if (tenure < 3) {
            return "ESTABLISHED (1 to 2 years)";
        } else {
            return "LOYAL (3+ years)";
        }
    }

    public double getMinimumBalance(String type, int tenure) {
        checkTenure(tenure);
        String t = normalize(type);
        if (t.equals("SAVINGS")) {
            if (tenure < 1) return 500;
            else if (tenure < 3) return 1000;
            else return 2000;
        } else if (t.equals("CURRENT")) {
            return 0; // current accounts rely on an overdraft instead of a minimum balance
        } else if (t.equals("FIXED_DEPOSIT")) {
            if (tenure < 3) return 5000;
            else return 10000;
        } else if (t.equals("SALARY")) {
            return 0; // zero-balance salary account
        }
        throw unknown(type);
    }

    public double getInterestRate(String type, int tenure) {
        checkTenure(tenure);
        String t = normalize(type);
        if (t.equals("SAVINGS")) {
            if (tenure < 1) return 2.5;
            else if (tenure < 3) return 3.5;
            else return 4.5;
        } else if (t.equals("CURRENT")) {
            return 0.0;
        } else if (t.equals("FIXED_DEPOSIT")) {
            if (tenure < 1) return 5.0;
            else if (tenure < 3) return 6.5;
            else return 7.0;
        } else if (t.equals("SALARY")) {
            if (tenure < 1) return 3.0;
            else if (tenure < 3) return 3.5;
            else return 4.0;
        }
        throw unknown(type);
    }

    public String getFeatureName(String type, int tenure) {
        checkTenure(tenure);
        String t = normalize(type);
        if (t.equals("SAVINGS")) {
            if (tenure < 1) return "Basic Savings";
            else if (tenure < 3) return "Regular Savings";
            else return "Premium Savings";
        } else if (t.equals("CURRENT")) {
            if (tenure < 1) return "Basic Current";
            else if (tenure < 3) return "Business Current";
            else return "Premier Current";
        } else if (t.equals("FIXED_DEPOSIT")) {
            if (tenure < 1) return "Short-Term FD";
            else if (tenure < 3) return "Standard FD";
            else return "Long-Term FD";
        } else if (t.equals("SALARY")) {
            if (tenure < 1) return "Salary Basic";
            else if (tenure < 3) return "Salary Plus";
            else return "Salary Elite";
        }
        throw unknown(type);
    }

    private static String normalize(String type) {
        if (type == null) {
            throw new IllegalArgumentException("Account type must not be null");
        }
        String t = type.trim().toUpperCase();
        return t.equals("FIXED") ? "FIXED_DEPOSIT" : t;
    }

    private static void checkTenure(int tenure) {
        if (tenure < 0) {
            throw new IllegalArgumentException("Tenure must not be negative: " + tenure);
        }
    }

    private static IllegalArgumentException unknown(String type) {
        return new IllegalArgumentException("Unknown account type: " + type);
    }
}
