package com.bank.api;

public interface ITransactionService {
    boolean transfer(String fromAccount, String toAccount, double amount);
    void printTransactionHistory(String accountNumber);
}