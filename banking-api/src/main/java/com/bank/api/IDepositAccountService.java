package com.bank.api;

import java.math.BigDecimal;

public interface IDepositAccountService {
    DepositAccount createDepositAccount(String identificationNo, String profilePassword, BigDecimal initialBalance);
    DepositAccount getDepositAccount(String identificationNo, String profilePassword);
    DepositAccount closeDepositAccount(String identificationNo, String profilePassword);
    DepositAccount depositFunds(String identificationNo, String profilePassword, BigDecimal amount);
    DepositAccount withdrawFunds(String identificationNo, String profilePassword, BigDecimal amount);
    DepositAccount updateDepositAccountStatus(String identificationNo, String profilePassword, String action);
}
