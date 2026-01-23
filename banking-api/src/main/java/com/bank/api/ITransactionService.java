package com.bank.api;

import com.bank.api.model.Transaction;
import com.bank.api.model.TransactionSummary;
import com.bank.api.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ITransactionService {

    boolean recordTransaction(
            String identificationNo,
            String password,
            TransactionType type,
            BigDecimal amount,
            String note
    );

    
    List<Transaction> getTransactionHistory(
            String identificationNo,
            String password
    );

    List<Transaction> filterTransactions(
            String identificationNo,
            String password,
            TransactionType type,
            LocalDateTime from,
            LocalDateTime to
    );

    TransactionSummary getTransactionSummary(
            String identificationNo,
            String password,
            LocalDateTime from,
            LocalDateTime to
    );

    boolean transfer(
            String fromIdentificationNo,
            String password,
            String toIdentificationNo,
            BigDecimal amount
    );
}
