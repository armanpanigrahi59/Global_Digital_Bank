# Activity map

The assignment was built up in stages (a basic account, then exceptions, then subclasses, and so on).
In this project the stages are merged into one design; this table says where each ended up.

| Activity | Topic | Now in |
|---|---|---|
| 1–2 | Basic account: encapsulation, deposit / withdraw | `domain/AbstractAccount`, `domain/IAccount` |
| 3–4 | Validation and custom exceptions | `exception/*`, validation in `AbstractAccount` |
| 5 | Exception hierarchy and its tests | `exception/*`, `AccountLifecycleTest`, `SavingsAccountTest` |
| 6 | Account types by inheritance | `SavingsAccount`, `CurrentAccount`, `FixedDepositAccount`, `SalaryAccount` |
| 7–8 | Abstraction, interface, polymorphism, minimum balance | `AbstractAccount`, `IAccount`, product tests |
| 9–10 | Factory, tenure-based rules, PIN, account lifecycle | `factory/AccountFactory`, `AccountLifecycleTest`, `AccountFactoryTest` |
| 11–12 | Rules engine singleton, properties-driven rules | `rules/*`, `resources/rules/*.properties`, `AccountRulesEngineTest` |
| 13 | Daily transfer limit, transfers | `domain/TransferService`, `TransferServiceTest` |
| 14–15 | Hot reload of rules, safe failure handling | `AccountRulesEngine.reloadRules / reloadIfChanged`, `PropertiesRulesLoaderTest` |
| 16 | Command pattern for transactions | `command/*`, `CommandTest` |
| 17 | Serialisable transaction log | `logging/TransactionLog`, `TransactionLogTest` |
| 18 | Bridge: file / memory / database destinations | `logging/*`, `LoggerBridgeTest` |
| 19 | Service layer and console UI | `service/AccountService`, `ui/AccountUI`, `AccountServiceTest`, `AccountUITest` |
| 20 | Composition root and runnable application | `GlobalDigitalBankApp`, `GlobalDigitalBankAppTest` |

The earlier per-activity source folders (`activity1-2` … `activity16-20`) are the learning-path
versions; they are replaced by the structure above and can be deleted or archived.
