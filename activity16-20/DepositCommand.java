/** Deposit money into an account. */
public class DepositCommand implements TransactionCommand {
    private static final long serialVersionUID = 1L;

    private final transient IAccount account;
    private final String accountNumber;
    private final double amount;
    private Transaction transaction;

    public DepositCommand(IAccount account, double amount) {
        this.account = account;
        this.accountNumber = account.getAccountNumber();
        this.amount = amount;
    }

    @Override
    public void execute() throws AccountException {
        if (account == null) {
            throw new IllegalStateException("Command was restored from a log and cannot be executed");
        }
        if (transaction != null) {
            throw new IllegalStateException("Command has already been executed");
        }
        transaction = account.depositWithTransaction(amount);
    }

    @Override
    public Transaction getTransaction() {
        return transaction;
    }

    public String getAccountNumber() { return accountNumber; }
    public double getAmount() { return amount; }
}
