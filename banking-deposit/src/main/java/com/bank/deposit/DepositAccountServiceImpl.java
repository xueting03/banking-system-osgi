package com.bank.deposit;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.bank.api.Customer;
import com.bank.api.DepositAccount;
import com.bank.api.ICustomerService;
import com.bank.api.IDepositAccountService;

public class DepositAccountServiceImpl implements IDepositAccountService {
    
    private final ICustomerService customerService;
    private final Map<String, DepositAccount> accounts = new HashMap<>();
    
    public DepositAccountServiceImpl(ICustomerService customerService) {
        this.customerService = customerService;
    }
    
    /**
     * Helper method to get deposit account by identification number
     */
    private DepositAccount getAccountByIdentificationNo(String identificationNo) {
        Customer customer = customerService.getCustomer(identificationNo);
        if (customer == null) {
            return null;
        }
        
        List<DepositAccount> customerAccounts = accounts.values().stream()
            .filter(account -> customer.getId().equals(account.getCustomerId()))
            .collect(Collectors.toList());
        return customerAccounts.isEmpty() ? null : customerAccounts.get(0);
    }
    
    /** Creates a new deposit account for an active customer with valid password who doesn't already have one. */
    @Override
    public synchronized String createDepositAccount(String identificationNo, String profilePassword, BigDecimal initialBalance) {
        // Validate inputs
        if (identificationNo == null || identificationNo.trim().isEmpty()) {
            return "ERROR: Identification number is required";
        }
        
        if (profilePassword == null || profilePassword.trim().isEmpty()) {
            return "ERROR: Profile password is required";
        }
        
        // Validate profile password using CustomerService
        if (!customerService.verifyLogin(identificationNo, profilePassword)) {
            return "ERROR: password is incorrect";
        }
        
        Customer customer = customerService.getCustomer(identificationNo);
        if (customer == null) {
            return "ERROR: Customer not found";
        }
        
        // Check if customer already has a deposit account
        List<DepositAccount> existingAccounts = accounts.values().stream()
            .filter(account -> customer.getId().equals(account.getCustomerId()))
            .collect(Collectors.toList());
        if (!existingAccounts.isEmpty()) {
            return "ERROR: deposit account already exists";
        }
        
        // Create deposit account with initial balance
        String accountId = "DA" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        BigDecimal startingBalance = (initialBalance != null && initialBalance.compareTo(BigDecimal.ZERO) >= 0) 
            ? initialBalance 
            : BigDecimal.ZERO;
        DepositAccount account = new DepositAccount(accountId, customer.getId(), startingBalance);
        accounts.put(accountId, account);
        
        System.out.println("Deposit account created successfully: " + account);
        return String.format("SUCCESS: Deposit account created (ID: %s, Initial Balance: $%s, Status: %s)", 
            accountId, account.getBalance(), account.getStatus());
    }
    
    /** Retrieves deposit account details for a customer with valid password. */
    @Override
    public synchronized String getDepositAccount(String identificationNo, String profilePassword) {
        // Validate inputs
        if (identificationNo == null || identificationNo.trim().isEmpty()) {
            return "ERROR: Identification number is required";
        }
        
        if (profilePassword == null || profilePassword.trim().isEmpty()) {
            return "ERROR: Profile password is required";
        }
        
        // Validate profile password using CustomerService
        if (!customerService.verifyLogin(identificationNo, profilePassword)) {
            return "ERROR: password is incorrect";
        }
        DepositAccount account = getAccountByIdentificationNo(identificationNo);
        if (account == null) {
            return "ERROR: no deposit account is found";
        }
        return String.format("SUCCESS: Account ID: %s, Status: %s, Balance: $%s, Created: %s", 
            account.getAccountId(), account.getStatus(), account.getBalance(), 
            account.getCreatedAt().toString());
    }
    
    /** Closes an existing deposit account with valid password authentication. */
    @Override
    public synchronized String closeDepositAccount(String identificationNo, String profilePassword) {
        // Validate inputs
        if (identificationNo == null || identificationNo.trim().isEmpty()) {
            return "ERROR: Identification number is required";
        }
        
        if (profilePassword == null || profilePassword.trim().isEmpty()) {
            return "ERROR: Profile password is required";
        }
        
        // Validate profile password using CustomerService
        if (!customerService.verifyLogin(identificationNo, profilePassword)) {
            return "ERROR: password is incorrect";
        }
        
        //Check if deposit account exists
        DepositAccount account = getAccountByIdentificationNo(identificationNo);
        if (account == null) {
            return "ERROR: no deposit account is found";
        }
        
        // Update status to Closed
        account.setStatus("Closed");
        accounts.put(account.getAccountId(), account);
        
        System.out.println("Deposit account closed: " + account.getAccountId());
        return String.format("SUCCESS: Deposit account closed (ID: %s, Final Balance: $%s)", 
            account.getAccountId(), account.getBalance());
    }
    
    /** Deposits funds into an active account with valid password and positive amount. */
    @Override
    public synchronized String depositFunds(String identificationNo, String profilePassword, BigDecimal amount) {
        // Validate inputs
        if (identificationNo == null || identificationNo.trim().isEmpty()) {
            return "ERROR: Identification number is required";
        }
        
        if (profilePassword == null || profilePassword.trim().isEmpty()) {
            return "ERROR: Profile password is required";
        }
        
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return "ERROR: Deposit amount must be greater than zero";
        }
        
        // Validate profile password using CustomerService
        if (!customerService.verifyLogin(identificationNo, profilePassword)) {
            return "ERROR: password is incorrect";
        }
        
        // Check if deposit account exists
        DepositAccount account = getAccountByIdentificationNo(identificationNo);
        if (account == null) {
            return "ERROR: no deposit account is found";
        }
        
        // Check if account is Active
        if (!account.isActive()) {
            return String.format("ERROR: Cannot deposit funds. Account status is %s (must be Active)", 
                account.getStatus());
        }
        
        // Credit amount
        BigDecimal oldBalance = account.getBalance();
        BigDecimal newBalance = oldBalance.add(amount);
        account.setBalance(newBalance);
        accounts.put(account.getAccountId(), account);
        
        System.out.printf("Deposited $%s to account %s%n", amount, account.getAccountId());
        return String.format("SUCCESS: Deposited $%s. Previous Balance: $%s, New Balance: $%s", 
            amount, oldBalance, newBalance);
    }
    
    /** Withdraws funds from an active account with valid password, positive amount, and sufficient balance. */
    @Override
    public synchronized String withdrawFunds(String identificationNo, String profilePassword, BigDecimal amount) {
        // Validate inputs
        if (identificationNo == null || identificationNo.trim().isEmpty()) {
            return "ERROR: Identification number is required";
        }
        
        if (profilePassword == null || profilePassword.trim().isEmpty()) {
            return "ERROR: Profile password is required";
        }
        
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return "ERROR: Withdrawal amount must be greater than zero";
        }
        
        // Validate profile password using CustomerService
        if (!customerService.verifyLogin(identificationNo, profilePassword)) {
            return "ERROR: password is incorrect";
        }
        
        // Check if deposit account exists
        DepositAccount account = getAccountByIdentificationNo(identificationNo);
        if (account == null) {
            return "ERROR: no deposit account is found";
        }
        
        // Check if account is Active
        if (!account.isActive()) {
            return String.format("ERROR: Cannot withdraw funds. Account status is %s (must be Active)", 
                account.getStatus());
        }
        
        // Validate sufficient balance
        if (account.getBalance().compareTo(amount) < 0) {
            return "ERROR: insufficient balance";
        }
        
        // Debit amount
        BigDecimal oldBalance = account.getBalance();
        BigDecimal newBalance = oldBalance.subtract(amount);
        account.setBalance(newBalance);
        accounts.put(account.getAccountId(), account);
        
        System.out.printf("Withdrew $%s from account %s%n", amount, account.getAccountId());
        return String.format("SUCCESS: Withdrew $%s. Previous Balance: $%s, New Balance: $%s", 
            amount, oldBalance, newBalance);
    }
    
    /** Freezes or unfreezes a non-closed account with valid password authentication. */
    @Override
    public synchronized String updateDepositAccountStatus(String identificationNo, String profilePassword, String action) {
        // Validate inputs
        if (identificationNo == null || identificationNo.trim().isEmpty()) {
            return "ERROR: Identification number is required";
        }
        
        if (profilePassword == null || profilePassword.trim().isEmpty()) {
            return "ERROR: Profile password is required";
        }
        
        if (action == null || (!action.equalsIgnoreCase("FREEZE") && !action.equalsIgnoreCase("UNFREEZE"))) {
            return "ERROR: Action must be either 'FREEZE' or 'UNFREEZE'";
        }
        
        // Validate profile password using CustomerService
        if (!customerService.verifyLogin(identificationNo, profilePassword)) {
            return "ERROR: password is incorrect";
        }
        
        // Check if deposit account exists
        DepositAccount account = getAccountByIdentificationNo(identificationNo);
        if (account == null) {
            return "ERROR: no deposit account is found";
        }
        
        // Check if account is Closed
        if (account.isClosed()) {
            return "ERROR: Cannot update status. Account is Closed";
        }
        
        // Process status update
        String currentStatus = account.getStatus();
        
        if (action.equalsIgnoreCase("FREEZE")) {
            if (account.isActive()) {
                account.setStatus("Frozen");
                accounts.put(account.getAccountId(), account);
                System.out.println("Account frozen: " + account.getAccountId());
                return String.format("SUCCESS: Account frozen (ID: %s, Previous Status: %s, New Status: Frozen)", 
                    account.getAccountId(), currentStatus);
            } else if (account.isFrozen()) {
                return "ERROR: Account is already Frozen";
            } else {
                return String.format("ERROR: Cannot freeze account with status: %s", currentStatus);
            }
        } else { // UNFREEZE
            if (account.isFrozen()) {
                account.setStatus("Active");
                accounts.put(account.getAccountId(), account);
                System.out.println("Account unfrozen: " + account.getAccountId());
                return String.format("SUCCESS: Account unfrozen (ID: %s, Previous Status: %s, New Status: Active)", 
                    account.getAccountId(), currentStatus);
            } else if (account.isActive()) {
                return "ERROR: Account is already Active";
            } else {
                return String.format("ERROR: Cannot unfreeze account with status: %s", currentStatus);
            }
        }
    }
}
