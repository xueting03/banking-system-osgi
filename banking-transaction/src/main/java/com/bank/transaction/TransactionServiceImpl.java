package com.bank.transaction;

import com.bank.api.IAccountService;
import com.bank.api.ITransactionService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionServiceImpl implements ITransactionService {
    
    private IAccountService accountService;
    private Map<String, List<String>> transactionHistory = new HashMap<>();
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public TransactionServiceImpl(IAccountService accountService) {
        this.accountService = accountService;
    }
    
    @Override
    public boolean transfer(String fromAccount, String toAccount, double amount) {
        System.out.printf("%nProcessing transfer: $%.2f from %s to %s%n", 
            amount, fromAccount, toAccount);
        
        if (accountService.getAccount(fromAccount) == null) {
            System.out.println("Source account not found: " + fromAccount);
            return false;
        }
        
        if (accountService.getAccount(toAccount) == null) {
            System.out.println("Destination account not found: " + toAccount);
            return false;
        }
        
        if (amount <= 0) {
            System.out.println("Invalid transfer amount: " + amount);
            return false;
        }
        
        // Withdraw from source
        if (!accountService.withdraw(fromAccount, amount)) {
            System.out.println("Transfer failed: Could not withdraw from source account");
            return false;
        }
        
        // Deposit to destination
        accountService.deposit(toAccount, amount);
        
        // Record transaction
        String timestamp = LocalDateTime.now().format(formatter);
        recordTransaction(fromAccount, String.format("[%s] Transferred $%.2f to %s", 
            timestamp, amount, toAccount));
        recordTransaction(toAccount, String.format("[%s] Received $%.2f from %s", 
            timestamp, amount, fromAccount));
        
        System.out.println("Transfer completed successfully!");
        return true;
    }
    
    @Override
    public void printTransactionHistory(String accountNumber) {
        System.out.println("%nTransaction History for " + accountNumber + ":");
        System.out.println("==========================================");
        
        List<String> history = transactionHistory.get(accountNumber);
        if (history == null || history.isEmpty()) {
            System.out.println("No transactions found");
        } else {
            for (String transaction : history) {
                System.out.println(transaction);
            }
        }
        System.out.println("==========================================");
    }
    
    private void recordTransaction(String accountNumber, String transaction) {
        transactionHistory.computeIfAbsent(accountNumber, k -> new ArrayList<>())
                         .add(transaction);
    }
}