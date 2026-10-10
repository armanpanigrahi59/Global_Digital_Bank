import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity 19 - The single entry point for banking operations.
 *
 * The UI (or any other client) talks only to this class. It keeps the
 * account registry, runs each money movement as a Command, and records every
 * successful movement through the TransactionLogger. Domain rules (limits,
 * minimum balances, PINs) stay in the domain classes; this layer
 * orchestrates them.
 *
 * Only successful operations are logged. If logging fails after a movement
 * succeeded, a TransactionLogException is thrown and the message says so.
 */
public class AccountService {
    private static final String ACCOUNT_PREFIX = "GDB";

    private final Map<String, IAccount> registry = new LinkedHashMap<>();
    private final TransactionLogger logger;
    private final TransferService transferService;
    private int nextAccountNumber = 1001;

    public AccountService(TransactionLogger logger) {
        this(logger, new TransferService());
    }

    public AccountService(TransactionLogger logger, TransferService transferService) {
        if (logger == null || transferService == null) {
            throw new IllegalArgumentException("Logger and transfer service are required");
        }
        this.logger = logger;
        this.transferService = transferService;
    }

    // ---- Accounts ----

    /**
     * Opens an account and registers it under a newly generated number.
     *
     * @param pin exactly four digits
     * @throws IllegalArgumentException for a bad PIN, unknown type, underage holder, or
     *                                  an opening balance below the type's minimum
     */
    public synchronized IAccount openAccount(String type, String holderName, int age,
                                             double initialBalance, String pin, int tenureYears) {
        if (holderName == null || holderName.isBlank()) {
            throw new IllegalArgumentException("Account holder name is required");
        }
        if (pin == null || !pin.matches("\\d{4}")) {
            throw new IllegalArgumentException("PIN must be exactly 4 digits");
        }
        String accountNumber = ACCOUNT_PREFIX + nextAccountNumber;
        IAccount account = AccountFactory.createAccount(type, accountNumber, holderName.trim(),
                age, initialBalance, pin, tenureYears);
        registry.put(accountNumber, account); // only reached if creation succeeded
        nextAccountNumber++;
        return account;
    }

    /** Closes the account (PIN required). It stays in the registry so its history remains available. */
    public synchronized void closeAccount(String accountNumber, String pin) throws AccountException {
        getAccount(accountNumber).closeAccount(pin);
    }

    public synchronized IAccount getAccount(String accountNumber) throws AccountNotFoundException {
        IAccount account = accountNumber == null ? null : registry.get(accountNumber.trim());
        if (account == null) {
            throw new AccountNotFoundException(accountNumber);
        }
        return account;
    }

    /** Balance lookup protected by the account PIN. */
    public synchronized double getBalance(String accountNumber, String pin) throws AccountException {
        IAccount account = getAccount(accountNumber);
        account.verifyPin(pin);
        return account.getBalance();
    }

    public synchronized List<IAccount> getAllAccounts() {
        return new ArrayList<>(registry.values());
    }

    // ---- Money movements ----

    public synchronized Transaction deposit(String accountNumber, double amount) throws AccountException {
        return run(new DepositCommand(getAccount(accountNumber), amount));
    }

    public synchronized Transaction withdraw(String accountNumber, double amount, String pin)
            throws AccountException {
        return run(new WithdrawCommand(getAccount(accountNumber), amount, pin));
    }

    public synchronized Transaction transfer(String fromAccountNumber, String toAccountNumber,
                                             double amount, String pin) throws AccountException {
        IAccount from = getAccount(fromAccountNumber);
        IAccount to = getAccount(toAccountNumber);
        return run(new TransferCommand(transferService, from, to, amount, pin));
    }

    // ---- History ----

    /** Transactions touching the account (as source or target), oldest first. PIN required. */
    public synchronized List<Transaction> getTransactionHistory(String accountNumber, String pin)
            throws AccountException {
        IAccount account = getAccount(accountNumber);
        account.verifyPin(pin);
        List<Transaction> history = new ArrayList<>();
        try {
            for (TransactionCommand command : logger.readAll()) {
                Transaction t = command.getTransaction();
                if (t != null && (t.getAccountNumber().equals(account.getAccountNumber())
                        || account.getAccountNumber().equals(t.getToAccountNumber()))) {
                    history.add(t);
                }
            }
        } catch (IOException e) {
            throw new TransactionLogException("Could not read the transaction log", e);
        }
        return history;
    }

    public synchronized TransactionLogger getLogger() {
        return logger;
    }

    // ---- Internals ----

    /** Executes the command; logs it only if it succeeded. */
    private Transaction run(TransactionCommand command) throws AccountException {
        command.execute(); // throws on any rule violation, nothing is logged
        Transaction transaction = command.getTransaction();
        try {
            logger.log(command);
        } catch (IOException e) {
            throw new TransactionLogException("Transaction " + transaction.getTransactionId()
                    + " was completed but could not be logged", e);
        }
        return transaction;
    }
}
