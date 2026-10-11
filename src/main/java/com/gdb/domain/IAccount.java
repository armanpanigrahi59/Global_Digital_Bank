package com.gdb.domain;

import com.gdb.exception.AccountException;
import com.gdb.exception.InvalidPinException;

/**
 * The contract every account exposes. Services, commands and the UI depend
 * on this interface and never on a concrete account class.
 */
public interface IAccount {
    String getAccountNumber();

    String getAccountHolderName();

    double getBalance();

    AccountType getAccountType();

    /** Whole years the customer has held this account; selects the rules tier. */
    int getTenureYears();

    AccountStatus getStatus();

    boolean isActive();

    double getMinimumBalance();

    /** Annual interest rate in percent. */
    double getInterestRate();

    /** True if this amount could be withdrawn right now; never changes anything. */
    boolean canWithdraw(double amount);

    void deposit(double amount) throws AccountException;

    void withdraw(double amount) throws AccountException;

    /** Same as {@link #deposit(double)} but returns the record of what happened. */
    Transaction depositWithTransaction(double amount) throws AccountException;

    /** Same as {@link #withdraw(double)} but returns the record of what happened. */
    Transaction withdrawWithTransaction(double amount) throws AccountException;

    /** Credits one month of interest (annual rate / 12) if the account is active. */
    void applyMonthlyInterest();

    void verifyPin(String pin) throws InvalidPinException;

    void closeAccount(String pin) throws AccountException;

    /** Moves a CLOSED or INACTIVE account back to ACTIVE. */
    void reopenAccount(String pin) throws AccountException;

    /** Moves an ACTIVE account to INACTIVE (dormant). */
    void deactivateAccount(String pin) throws AccountException;
}
