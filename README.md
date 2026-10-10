# Global Digital Bank – Activities 1 to 10 (Java)

Each folder is self-contained (no packages, no dependencies). Requires JDK 11+.
From inside a folder: `javac -d out *.java` then `java -cp out <MainClass>`.

| Folder | Activities | Main class to run |
|---|---|---|
| activity1-2 | 1 Account, 2 TestAccount | `TestAccount` |
| activity3-4 | 3 AccountEnhanced, 4 TestAccountEnhanced | `TestAccountEnhanced` |
| activity5 | 5 Account with custom exceptions, 6 exception tests | `DemoAccountExceptions`, `TestAccountExceptions` |
| activity7-8 | 7 SavingsAccount / CurrentAccount, 8 subclass tests | `TestAccountSubclasses` |
| activity9-10 | 9 abstract BankAccount (+ FixedDepositAccount), 10 abstract tests | `TestAbstractAccount` |

Test classes print `[PASS]`/`[FAIL]` per check and exit with code 1 if any check fails.

## Design notes
- Activity 9 renames the base class to `BankAccount` (abstract). `withdraw()` and
  `applyMonthlyInterest()` are template methods built on the abstract hooks
  `getMinimumBalance()`, `getAccountType()`, `getInterestRate()` and `canWithdraw()`.
- Monthly interest = annual rate / 12.
- Current accounts use an overdraft instead of a minimum balance; fixed deposits can be
  withdrawn from only after `markMatured()`.
