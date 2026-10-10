# Global Digital Bank – Activities 1 to 20 (Java)

Each folder is self-contained (no packages, no dependencies). Requires JDK 17+.
From inside a folder: `javac -d out *.java` then `java -cp out <MainClass>`.

| Folder | Activities | Main class to run |
|---|---|---|
| activity1-2 | 1 Account, 2 TestAccount | `TestAccount` |
| activity3-4 | 3 AccountEnhanced, 4 TestAccountEnhanced | `TestAccountEnhanced` |
| activity5 | 5 Account with custom exceptions, 6 exception tests | `DemoAccountExceptions`, `TestAccountExceptions` |
| activity7-8 | 7 SavingsAccount / CurrentAccount, 8 subclass tests | `TestAccountSubclasses` |
| activity9-10 | 9 abstract BankAccount (+ FixedDepositAccount), 10 abstract tests | `TestAbstractAccount` |
| activity11-12 | 11 IAccount, AbstractAccount, AccountFactory, SalaryAccount; 12 tests | `TestInterfaceFactory` |
| activity13-1 | 13.1 rules engine with if-else | `TestAccountRulesEngine` |
| activity13-2 | 13.2 rules engine with nested Map (same test, same results) | `TestAccountRulesEngine` |
| activity14-15 | 14 rules from `.properties` + hot reload; 15 TransferService with daily limits | `TestRulesProperties`, `TestTransferService` |
| activity16-20 | 16 Transaction model, 17 Command pattern + file log, 18 Bridge logging, 19 AccountService, 20 console UI | **`Main`** (the bank), plus the tests below |

Test classes print `[PASS]`/`[FAIL]` per check and exit with code 1 if any check fails.
**Run the activity14-15 and activity16-20 tests from inside their folder** – rules are read from `./config`.

## Running the bank (activity16-20)
```
cd activity16-20
javac -d out *.java
java -cp out Main            # logs to transactions.log
java -cp out Main memory     # in-memory log        (also: db = simulated database)
```
Menu: open account, deposit, withdraw, transfer, view balance, close account, view transactions.
Account numbers are generated (GDB1001, GDB1002, ...). PINs are exactly 4 digits. Accounts live in
memory only, so they are gone when the program exits; only the transaction log can persist.

Tests in activity16-20: `TestTransactionModel` (16), `TestCommandLogging` (17),
`TestBridgeLogging` (18), `TestAccountService` (19), `TestAccountUI` (20), plus the earlier
`TestTransferService` and `TestRulesProperties` as regression checks.

Activities 16-20 build on each other (the PDF's own diagram combines 16 and 17), so they live in
one cumulative folder. Files by activity:
- 16: `Transaction`, `TransactionType`; `depositWithTransaction` / `withdrawWithTransaction` on accounts;
  `TransferService.transfer` now returns a `Transaction`
- 17: `TransactionCommand`, `DepositCommand`, `WithdrawCommand`, `TransferCommand`, `TransactionLog`
- 18: `LogDestination`, `FileLogDestination`, `MemoryLogDestination`, `DatabaseLogDestination`,
  `SimulatedDatabase`, `TransactionLogger`
- 19: `AccountService`, `AccountNotFoundException`, `TransactionLogException`
- 20: `AccountUI`, `Main`

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
- Activity 15 daily limits are per source account, set by account type and tenure, and reset when the
  date changes. Transfers are all-or-nothing.
- Activity 17 logs commands with Java serialization. Account objects and PINs are `transient`, so
  they never reach the file, and a command read back from the log is a history record that cannot be
  executed again. Appending uses a header-less stream (a plain `ObjectOutputStream` per save would
  corrupt the file), and reading only accepts this project's classes.
- Activity 19 logs only successful operations. If the log write fails after money has moved, a
  `TransactionLogException` says the transaction completed but was not logged.
