package com.bank.api;

import java.math.BigDecimal;

public interface IDepositAccountService {
    String createDepositAccount(String identificationNo, String profilePassword, BigDecimal initialBalance);
    String getDepositAccount(String identificationNo, String profilePassword);
    String closeDepositAccount(String identificationNo, String profilePassword);
    String depositFunds(String identificationNo, String profilePassword, BigDecimal amount);
    String withdrawFunds(String identificationNo, String profilePassword, BigDecimal amount);
    String updateDepositAccountStatus(String identificationNo, String profilePassword, String action);
}
