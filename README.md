# Global Digital Bank

A console banking system in Java: savings, current, fixed-deposit and salary accounts, business rules
loaded from `.properties` files (and hot-reloadable), PIN-protected deposits / withdrawals / transfers
with daily limits, and a transaction log that can go to a file, memory or a simulated database.

This is a standard Maven project. It is the consolidated result of the 20-activity assignment — see
[docs/ACTIVITIES.md](docs/ACTIVITIES.md) for where each activity now lives.

## Requirements

- JDK 17 or newer
- Maven 3.8+

## Build, test, run

```bash
mvn test                       # compile and run the JUnit 5 suite
mvn package                    # builds target/global-digital-bank.jar (and runs the tests)

java -jar target/global-digital-bank.jar            # log to data/transactions.log
java -jar target/global-digital-bank.jar memory     # log kept in memory only
java -jar target/global-digital-bank.jar db         # log kept in the simulated database

# use your own edited rules instead of the packaged ones
java -Dgdb.rules.dir=./config -jar target/global-digital-bank.jar
```

Without Maven, plain `javac` works too:

```bash
javac -d out $(find src/main/java -name '*.java') && cp -r src/main/resources/* out/
java -cp out com.gdb.GlobalDigitalBankApp memory
```

## Project layout

```
pom.xml
src/
  main/
    java/com/gdb/
      GlobalDigitalBankApp.java     entry point and composition root
      exception/                    AccountException and its specific subclasses
      domain/                       accounts, transactions, transfer rules (no I/O)
      rules/                        rules engine + .properties loader
      factory/                      AccountFactory
      command/                      Deposit / Withdraw / Transfer commands
      logging/                      TransactionLogger and its destinations
      service/                      AccountService (the application API)
      ui/                           console menu
    resources/rules/                packaged rules: savings, current, fixeddeposit, salary
  test/java/com/gdb/                mirrors main, plus support/Fixtures
docs/
  ARCHITECTURE.md                   layers, dependencies, design decisions
  ACTIVITIES.md                     activity-to-code map
```

## Editing the rules

Rules live in `src/main/resources/rules/*.properties`. Keys look like
`savings.tenure.1.minBalance=1000`; the number after `tenure.` is the lowest customer tenure in years
that the row applies to (0 = under 1 year, 1 = 1 to under 3, 3 = 3 and more). Tier 0 is required.
Supported values per tier: `minBalance`, `interestRate`, `featureName`, `dailyTransferLimit`, and any
`feature.<name>` extras (e.g. `overdraftLimit`, `monthlyCreditLimit`).

To change rules without rebuilding, copy the files to a folder and start with `-Dgdb.rules.dir=<folder>`.
A file that fails validation never replaces working rules.

## Design notes

- **Rules are injected**, not looked up: accounts and services depend on the `AccountRules` interface;
  `AccountRulesEngine` (a singleton) is the production implementation and is wired in only in `GlobalDigitalBankApp`.
- **Account behaviour** shares one `AbstractAccount`; products only override a small hook
  (overdraft, maturity lock).
- **Transfers** are atomic: if the credit fails the debit is reversed, and a failed transfer does not
  use up the daily limit.
- **Logging never stores secrets**: commands are serialised with the PIN and live account objects marked
  `transient`, and reading a log only accepts the expected classes.
- **Persisted log entries are history**: a command read back from the log cannot be executed again.
