package com.bank.api;

public interface IAccountService {
    Account createAccount(String accountNumber, String ownerName, double initialBalance);
    Account getAccount(String accountNumber);
    double getBalance(String accountNumber);
    void deposit(String accountNumber, double amount);
    boolean withdraw(String accountNumber, double amount);
}