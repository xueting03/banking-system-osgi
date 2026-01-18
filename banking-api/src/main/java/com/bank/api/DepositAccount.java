package com.bank.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DepositAccount {
    
    private String accountId;
    private String customerId;
    private String status; // "Active", "Frozen", "Closed"
    private BigDecimal balance;
    private LocalDateTime createdAt;
    
    public DepositAccount() {}
    
    public DepositAccount(String accountId, String customerId) {
        this.accountId = accountId;
        this.customerId = customerId;
        this.status = "Active";
        this.balance = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
    }
    
    public DepositAccount(String accountId, String customerId, BigDecimal initialBalance) {
        this.accountId = accountId;
        this.customerId = customerId;
        this.status = "Active";
        this.balance = initialBalance != null ? initialBalance : BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
    }
    
    public String getAccountId() {
        return accountId;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public BigDecimal getBalance() {
        return balance;
    }
    
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public boolean isActive() {
        return "Active".equals(status);
    }
    
    public boolean isFrozen() {
        return "Frozen".equals(status);
    }
    
    public boolean isClosed() {
        return "Closed".equals(status);
    }
    
    @Override
    public String toString() {
        return String.format("DepositAccount[ID: %s, Customer: %s, Status: %s, Balance: $%s]", 
            accountId, customerId, status, balance);
    }
}
