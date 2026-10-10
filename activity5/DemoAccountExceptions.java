/** Small demo showing each custom exception being thrown and caught (Activity 5). */
public class DemoAccountExceptions {
    public static void main(String[] args) {
        Account acc = new Account("A001", "Alice", 30, 1000.0, "1234");

        try { acc.deposit(-5); }
        catch (AccountException e) { show(e); }

        try { acc.withdraw(5000); }
        catch (AccountException e) { show(e); }

        try { acc.withdraw(700); }
        catch (AccountException e) { show(e); }

        try { acc.verifyPin("0000"); }
        catch (AccountException e) { show(e); }

        try {
            acc.closeAccount("1234");
            acc.deposit(100);
        } catch (AccountException e) { show(e); }
    }

    private static void show(AccountException e) {
        System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
    }
}
