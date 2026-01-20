package com.bank.api;

import java.time.LocalDateTime;

public class Card {
    private String id;
    private String accountId;
    private String cardNumber;
    private int transactionLimit;
    private CardStatus status;
    private String pinNumber;
    private LocalDateTime createdAt;

    public enum CardStatus {
        ACTIVE,
        INACTIVE,
        FROZEN
    }

    public Card() {}

    public Card(String id, String accountId, String cardNumber, int transactionLimit, CardStatus status, String pinNumber) {
        this.id = id;
        this.accountId = accountId;
        this.cardNumber = cardNumber;
        this.transactionLimit = transactionLimit;
        this.status = status;
        this.pinNumber = pinNumber;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public int getTransactionLimit() { return transactionLimit; }
    public void setTransactionLimit(int transactionLimit) { this.transactionLimit = transactionLimit; }
    public CardStatus getStatus() { return status; }
    public void setStatus(CardStatus status) { this.status = status; }
    public String getPinNumber() { return pinNumber; }
    public void setPinNumber(String pinNumber) { this.pinNumber = pinNumber; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
