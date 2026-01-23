package com.bank.api;

import java.time.LocalDateTime;

public class Transaction {
    private String id;
    private String depositAccountId;
    private double amount;
    private String type; // DEPOSIT, WITHDRAWAL, TRANSFER
    private String description;
    private LocalDateTime timestamp;

    public Transaction() {}

    public Transaction(String id, String depositAccountId, double amount, String type, String description, LocalDateTime timestamp) {
        this.id = id;
        this.depositAccountId = depositAccountId;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDepositAccountId() { return depositAccountId; }
    public void setDepositAccountId(String depositAccountId) { this.depositAccountId = depositAccountId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
