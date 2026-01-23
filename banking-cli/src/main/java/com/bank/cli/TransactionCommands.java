package com.bank.cli;

import com.bank.api.ITransactionService;
import com.bank.api.model.Transaction;
import com.bank.api.model.TransactionSummary;
import com.bank.api.model.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class TransactionCommands {
    private ITransactionService transactionService;

    public TransactionCommands(ITransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // Record a new transaction
    public void record(String identificationNo, String password, TransactionType type, BigDecimal amount, String note) {
        boolean success = transactionService.recordTransaction(identificationNo, password, type, amount, note);
        System.out.println(success ? "Transaction recorded." : "Transaction failed.");
    }

    // Get transaction history
    public void history(String identificationNo, String password) {
        List<Transaction> history = transactionService.getTransactionHistory(identificationNo, password);
        System.out.println("Transaction history: " + history);
    }

    // Filter transactions
    public void filter(String identificationNo, String password, TransactionType type, LocalDateTime from, LocalDateTime to) {
        List<Transaction> filtered = transactionService.filterTransactions(identificationNo, password, type, from, to);
        System.out.println("Filtered transactions: " + filtered);
    }

    // Get transaction summary
    public void summary(String identificationNo, String password, LocalDateTime from, LocalDateTime to) {
        TransactionSummary summary = transactionService.getTransactionSummary(identificationNo, password, from, to);
        System.out.println("Transaction summary: " + summary);
    }

    // Transfer funds
    public void transfer(String fromIdentificationNo, String password, String toIdentificationNo, BigDecimal amount) {
        boolean success = transactionService.transfer(fromIdentificationNo, password, toIdentificationNo, amount);
        System.out.println(success ? "Transfer successful." : "Transfer failed.");
    }
}
