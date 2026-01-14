package com.bank.account;

import com.bank.api.Account;
import com.bank.api.IAccountService;
import java.util.HashMap;
import java.util.Map;

public class AccountServiceImpl implements IAccountService {
    
    private Map<String, Account> accounts = new HashMap<>();
    
    @Override
    public Account createAccount(String accountNumber, String ownerName, double initialBalance) {
        if (accounts.containsKey(accountNumber)) {
            System.out.println("Account already exists: " + accountNumber);
            return null;
        }
        
        Account account = new Account(accountNumber, ownerName, initialBalance);
        accounts.put(accountNumber, account);
        System.out.println("Account created: " + account);
        return account;
    }
    
    @Override
    public Account getAccount(String accountNumber) {
        return accounts.get(accountNumber);
    }
    
    @Override
    public double getBalance(String accountNumber) {
        Account account = accounts.get(accountNumber);
        if (account == null) {
            System.out.println("Account not found: " + accountNumber);
            return -1;
        }
        return account.getBalance();
    }
    
    @Override
    public void deposit(String accountNumber, double amount) {
        Account account = accounts.get(accountNumber);
        if (account == null) {
            System.out.println("Account not found: " + accountNumber);
            return;
        }
        
        if (amount <= 0) {
            System.out.println("Invalid deposit amount: " + amount);
            return;
        }
        
        account.setBalance(account.getBalance() + amount);
        System.out.printf("Deposited $%.2f to %s. New balance: $%.2f%n", 
            amount, accountNumber, account.getBalance());
    }
    
    @Override
    public boolean withdraw(String accountNumber, double amount) {
        Account account = accounts.get(accountNumber);
        if (account == null) {
            System.out.println("Account not found: " + accountNumber);
            return false;
        }
        
        if (amount <= 0) {
            System.out.println("Invalid withdrawal amount: " + amount);
            return false;
        }
        
        if (account.getBalance() < amount) {
            System.out.println("Insufficient funds in account: " + accountNumber);
            return false;
        }
        
        account.setBalance(account.getBalance() - amount);
        System.out.printf("Withdrew $%.2f from %s. New balance: $%.2f%n", 
            amount, accountNumber, account.getBalance());
        return true;
    }
}