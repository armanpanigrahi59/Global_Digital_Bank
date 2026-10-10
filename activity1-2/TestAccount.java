/**
 * Activity 2 - Test driver for the basic Account class.
 */
public class TestAccount {
    public static void main(String[] args) {
        Account acc = new Account("A001", "Alice", 1000.0);
        System.out.println("Account : " + acc.getAccountNumber() + " (" + acc.getAccountHolderName() + ")");
        System.out.printf("Opening balance : %.2f%n", acc.getBalance());

        acc.deposit(500.0);
        System.out.printf("After deposit 500 : %.2f%n", acc.getBalance());

        acc.withdraw(200.0);
        System.out.printf("After withdraw 200 : %.2f%n", acc.getBalance());

        check("Final balance is 1300.00", acc.getBalance() == 1300.0);

        acc.setAccountHolderName("Alice Smith");
        check("Holder name updated", "Alice Smith".equals(acc.getAccountHolderName()));
    }

    private static void check(String label, boolean ok) {
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + label);
    }
}
