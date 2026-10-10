# Global Digital Bank – Activities 1 to 15 (Java)

Each folder is self-contained (no packages, no dependencies). Requires JDK 11+ (JDK 17+ recommended).
From inside a folder: `javac -d out *.java` then `java -cp out <MainClass>`.

| Folder | Activities | Main class to run |
|---|---|---|
| activity1-2 | 1 Account, 2 TestAccount | `TestAccount` |
| activity3-4 | 3 AccountEnhanced, 4 TestAccountEnhanced | `TestAccountEnhanced` |
| activity5 | 5 Account with custom exceptions, 6 exception tests | `DemoAccountExceptions`, `TestAccountExceptions` |
| activity7-8 | 7 SavingsAccount / CurrentAccount, 8 subclass tests | `TestAccountSubclasses` |
| activity9-10 | 9 abstract BankAccount (+ FixedDepositAccount), 10 abstract tests | `TestAbstractAccount` |
| activity11-12 | 11 IAccount, AbstractAccount, AccountFactory, SalaryAccount; 12 interface/factory tests | `TestInterfaceFactory` |
| activity13-1 | 13.1 rules engine with if-else | `TestAccountRulesEngine` |
| activity13-2 | 13.2 rules engine with nested Map (same test, same results) | `TestAccountRulesEngine` |
| activity14-15 | 14 rules from `.properties` + hot reload; 15 TransferService with daily limits | `TestRulesProperties`, `TestTransferService` |

Test classes print `[PASS]`/`[FAIL]` per check and exit with code 1 if any check fails.
**Run the activity14-15 tests from inside that folder** – the rules are read from `./config`.

## Design notes
- Activity 9 renames the base class to `BankAccount` (abstract); Activity 11 renames it again to
  `AbstractAccount` and adds the `IAccount` interface. `withdraw()` and `applyMonthlyInterest()` are
  template methods built on the abstract hooks. Monthly interest = annual rate / 12.
- Rules (13.x, 14) depend on account type and tenure bucket: 0 = under 1 year, 1 = 1 to under 3
  years, 3 = 3 years or more.
- Activity 14 properties key format: `<type>.tenure.<fromYears>.<key>=<value>` with keys
  `minBalance`, `interestRate`, `featureName`, `dailyTransferLimit` and any `feature.<name>` extras
  (e.g. `overdraftLimit`, `monthlyCreditLimit`). A missing file falls back to built-in defaults; a
  malformed file makes a reload fail and the previous rules stay active.
- From Activity 14 accounts read minimum balance, interest rate and extras from the engine, so
  editing a `.properties` file and calling `reloadRules()` / `reloadIfChanged()` changes behaviour
  without a restart.
- Activity 15 daily limits are per source account, set by account type and tenure (a longer-standing
  customer gets a higher limit), and reset when the date changes. Transfers are all-or-nothing.
