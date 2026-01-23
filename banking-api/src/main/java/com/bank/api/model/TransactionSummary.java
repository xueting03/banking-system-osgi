package com.bank.api.model;

import java.math.BigDecimal;

public class TransactionSummary {

    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    private BigDecimal netAmount;

    public TransactionSummary() {
        this.totalDeposits = BigDecimal.ZERO;
        this.totalWithdrawals = BigDecimal.ZERO;
        this.netAmount = BigDecimal.ZERO;
    }

    public TransactionSummary(BigDecimal totalDeposits,
                              BigDecimal totalWithdrawals,
                              BigDecimal netAmount) {
        this.totalDeposits = totalDeposits;
        this.totalWithdrawals = totalWithdrawals;
        this.netAmount = netAmount;
    }

    public BigDecimal getTotalDeposits() {
        return totalDeposits;
    }

    public void setTotalDeposits(BigDecimal totalDeposits) {
        this.totalDeposits = totalDeposits;
    }

    public BigDecimal getTotalWithdrawals() {
        return totalWithdrawals;
    }

    public void setTotalWithdrawals(BigDecimal totalWithdrawals) {
        this.totalWithdrawals = totalWithdrawals;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }
}
