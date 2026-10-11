package com.gdb.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gdb.domain.AccountType;
import java.io.StringReader;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class PropertiesRulesLoaderTest {

    private Map<Integer, AccountRule> parse(String text) throws Exception {
        Properties p = new Properties();
        p.load(new StringReader(text));
        return PropertiesRulesLoader.parse(AccountType.SAVINGS, p, "test");
    }

    private String message(String text) {
        return assertThrows(IllegalStateException.class, () -> parse(text)).getMessage();
    }

    @Test
    void parsesTiersAndOptionalValues() throws Exception {
        Map<Integer, AccountRule> rules = parse(
                "savings.tenure.0.minBalance=100\nsavings.tenure.0.interestRate=1.5\n"
                + "savings.tenure.2.minBalance=200\nsavings.tenure.2.interestRate=2\n"
                + "savings.tenure.2.featureName=Gold\nsavings.tenure.2.dailyTransferLimit=77\n"
                + "savings.tenure.2.feature.bonus=12.5\nsavings.tenure.2.feature.label=VIP\n");
        assertEquals(2, rules.size());
        AccountRule base = rules.get(0);
        assertEquals(100.0, base.getMinBalance());
        assertEquals(1.5, base.getInterestRate());
        assertEquals(PropertiesRulesLoader.DEFAULT_DAILY_LIMIT, base.getDailyTransferLimit());
        AccountRule gold = rules.get(2);
        assertEquals("Gold", gold.getFeatureName());
        assertEquals(77.0, gold.getDailyTransferLimit());
        assertEquals(12.5, gold.getAdditionalFeatures().get("bonus"));
        assertEquals("VIP", gold.getAdditionalFeatures().get("label"));
    }

    @Test
    void tierZeroIsMandatory() {
        assertTrue(message("savings.tenure.1.minBalance=1\nsavings.tenure.1.interestRate=1\n").contains("tier 0"));
        assertTrue(message("").contains("tier 0"));
    }

    @Test
    void reportsMissingAndInvalidValues() {
        assertTrue(message("savings.tenure.0.interestRate=1\n").contains("savings.tenure.0.minBalance"));
        assertTrue(message("savings.tenure.0.minBalance=x\nsavings.tenure.0.interestRate=1\n").contains("minBalance"));
        assertTrue(message("savings.tenure.0.minBalance=-1\nsavings.tenure.0.interestRate=1\n").contains("minBalance"));
        assertTrue(message("savings.tenure.0.minBalance=NaN\nsavings.tenure.0.interestRate=1\n").contains("minBalance"));
        assertTrue(message("savings.tenure.a.minBalance=1\n").contains("bad tenure tier"));
    }

    @Test
    void packagedFilesLoadForEveryProduct() throws Exception {
        for (AccountType type : AccountType.values()) {
            assertTrue(PropertiesRulesLoader.load(type, null).containsKey(0), type.name());
        }
    }
}
