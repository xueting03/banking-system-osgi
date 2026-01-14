package com.bank.api;

public class Account {
    private String accountNumber;
    private String ownerName;
    private double balance;
    
    public Account(String accountNumber, String ownerName, double initialBalance) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.balance = initialBalance;
    }
    
    public String getAccountNumber() {
        return accountNumber;
    }
    
    public String getOwnerName() {
        return ownerName;
    }
    
    public double getBalance() {
        return balance;
    }
    
    public void setBalance(double balance) {
        this.balance = balance;
    }
    
    @Override
    public String toString() {
        return String.format("Account[%s, Owner: %s, Balance: $%.2f]", 
            accountNumber, ownerName, balance);
    }
}