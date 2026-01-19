package com.bank.deposit;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import javax.sql.DataSource;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.bank.api.Customer;
import com.bank.api.DepositAccount;
import com.bank.api.ICustomerService;
import com.bank.api.IDepositAccountService;

@Component(service = IDepositAccountService.class, immediate = true)
public class DepositAccountServiceImpl implements IDepositAccountService {
    
    @Reference
    private ICustomerService customerService;
    
    @Reference
    private DataSource dataSource;
    
    @Activate
    void activate() {
        try (Connection connection = dataSource.getConnection()) {
            initSchema(connection);
            System.out.println("=== Deposit Account Service Activated ===");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize DepositAccountServiceImpl", e);
        }
    }
    
    private void initSchema(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS DEPOSIT_ACCOUNT ("
            + "ACCOUNT_ID VARCHAR(36) PRIMARY KEY, "
            + "CUSTOMER_ID VARCHAR(255) NOT NULL, "
            + "BALANCE DECIMAL(19, 2) NOT NULL DEFAULT 0.00, "
            + "STATUS VARCHAR(32) NOT NULL DEFAULT 'Active', "
            + "CREATED_AT TIMESTAMP NOT NULL"
            + ")";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }
    
    private DepositAccount getAccountByIdentificationNo(String identificationNo) {
        Customer customer = customerService.getCustomer(identificationNo);
        if (customer == null) {
            return null;
        }
        
        String sql = "SELECT ACCOUNT_ID, CUSTOMER_ID, BALANCE, STATUS, CREATED_AT FROM DEPOSIT_ACCOUNT WHERE CUSTOMER_ID = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, customer.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapAccount(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Failed to retrieve account: " + e.getMessage());
        }
        return null;
    }
    
    private DepositAccount mapAccount(ResultSet rs) throws SQLException {
        DepositAccount account = new DepositAccount(
            rs.getString("ACCOUNT_ID"),
            rs.getString("CUSTOMER_ID"),
            rs.getBigDecimal("BALANCE")
        );
        account.setStatus(rs.getString("STATUS"));
        return account;
    }
    
    // Creates a new deposit account
    @Override
    public synchronized DepositAccount createDepositAccount(String identificationNo, String profilePassword, BigDecimal initialBalance) {
        if (identificationNo == null || identificationNo.trim().isEmpty()) {
            System.out.println("Deposit account creation failed: Identification number is required");
            return null;
        }
        
        if (profilePassword == null || profilePassword.trim().isEmpty()) {
            System.out.println("Deposit account creation failed: Profile password is required");
            return null;
        }
        
        if (!customerService.verifyLogin(identificationNo, profilePassword)) {
            System.out.println("Deposit account creation failed: password is incorrect");
            return null;
        }
        
        Customer customer = customerService.getCustomer(identificationNo);
        if (customer == null) {
            System.out.println("Deposit account creation failed: Customer not found");
            return null;
        }
        
        // Check if customer already has a deposit account
        DepositAccount existingAccount = getAccountByIdentificationNo(identificationNo);
        if (existingAccount != null) {
            System.out.println("Deposit account creation failed: deposit account already exists");
            return null;
        }
        
        // Create deposit account with initial balance
        String accountId = "DA" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        BigDecimal startingBalance = (initialBalance != null && initialBalance.compareTo(BigDecimal.ZERO) >= 0) 
            ? initialBalance 
            : BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();
        
        String sql = "INSERT INTO DEPOSIT_ACCOUNT (ACCOUNT_ID, CUSTOMER_ID, BALANCE, STATUS, CREATED_AT) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, accountId);
            ps.setString(2, customer.getId());
            ps.setBigDecimal(3, startingBalance);
            ps.setString(4, "Active");
            ps.setTimestamp(5, Timestamp.valueOf(now));
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Deposit account creation failed: " + e.getMessage());
            return null;
        }
        
        System.out.println("Deposit account created successfully: " + accountId);
        return getAccountByIdentificationNo(identificationNo);
    }
    
    // Retrieves deposit account details
    @Override
    public synchronized DepositAccount getDepositAccount(String identificationNo, String profilePassword) {
        if (identificationNo == null || identificationNo.trim().isEmpty()) {
            System.out.println("Get deposit account failed: Identification number is required");
            return null;
        }
        
        if (profilePassword == null || profilePassword.trim().isEmpty()) {
            System.out.println("Get deposit account failed: Profile password is required");
            return null;
        }
        
        if (!customerService.verifyLogin(identificationNo, profilePassword)) {
            System.out.println("Get deposit account failed: password is incorrect");
            return null;
        }
        DepositAccount account = getAccountByIdentificationNo(identificationNo);
        if (account == null) {
            System.out.println("Get deposit account failed: no deposit account is found");
            return null;
        }
        return account;
    }
    
    // Closes existing deposit account
    @Override
    public synchronized DepositAccount closeDepositAccount(String identificationNo, String profilePassword) {
        if (identificationNo == null || identificationNo.trim().isEmpty()) {
            System.out.println("Account closure failed: Identification number is required");
            return null;
        }
        
        if (profilePassword == null || profilePassword.trim().isEmpty()) {
            System.out.println("Account closure failed: Profile password is required");
            return null;
        }
        
        if (!customerService.verifyLogin(identificationNo, profilePassword)) {
            System.out.println("Account closure failed: password is incorrect");
            return null;
        }
        
        //Check if deposit account exists
        DepositAccount account = getAccountByIdentificationNo(identificationNo);
        if (account == null) {
            System.out.println("Account closure failed: no deposit account is found");
            return null;
        }
        
        // Update status to Closed
        String sql = "UPDATE DEPOSIT_ACCOUNT SET STATUS = ? WHERE ACCOUNT_ID = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, "Closed");
            ps.setString(2, account.getAccountId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Account closure failed: " + e.getMessage());
            return null;
        }
        
        System.out.println("Deposit account closed: " + account.getAccountId());
        account.setStatus("Closed");
        return account;
    }
    
    // Deposits funds
    @Override
    public synchronized DepositAccount depositFunds(String identificationNo, String profilePassword, BigDecimal amount) {
        if (identificationNo == null || identificationNo.trim().isEmpty()) {
            System.out.println("Deposit failed: Identification number is required");
            return null;
        }
        
        if (profilePassword == null || profilePassword.trim().isEmpty()) {
            System.out.println("Deposit failed: Profile password is required");
            return null;
        }
        
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("Deposit failed: Deposit amount must be greater than zero");
            return null;
        }
        
        if (!customerService.verifyLogin(identificationNo, profilePassword)) {
            System.out.println("Deposit failed: password is incorrect");
            return null;
        }
        
        // Check if deposit account exists
        DepositAccount account = getAccountByIdentificationNo(identificationNo);
        if (account == null) {
            System.out.println("Deposit failed: no deposit account is found");
            return null;
        }
        
        // Check if account is Active
        if (!account.isActive()) {
            System.out.printf("Deposit failed: Cannot deposit funds. Account status is %s (must be Active)%n", 
                account.getStatus());
            return null;
        }
        
        // Credit amount
        BigDecimal oldBalance = account.getBalance();
        BigDecimal newBalance = oldBalance.add(amount);
        
        String sql = "UPDATE DEPOSIT_ACCOUNT SET BALANCE = ? WHERE ACCOUNT_ID = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setString(2, account.getAccountId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Deposit failed: " + e.getMessage());
            return null;
        }
        
        System.out.printf("Deposited $%s to account %s%n", amount, account.getAccountId());
        account.setBalance(newBalance);
        return account;
    }
    
    // Withdraws funds
    @Override
    public synchronized DepositAccount withdrawFunds(String identificationNo, String profilePassword, BigDecimal amount) {
        if (identificationNo == null || identificationNo.trim().isEmpty()) {
            System.out.println("Withdrawal failed: Identification number is required");
            return null;
        }
        
        if (profilePassword == null || profilePassword.trim().isEmpty()) {
            System.out.println("Withdrawal failed: Profile password is required");
            return null;
        }
        
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("Withdrawal failed: Withdrawal amount must be greater than zero");
            return null;
        }
        
        if (!customerService.verifyLogin(identificationNo, profilePassword)) {
            System.out.println("Withdrawal failed: password is incorrect");
            return null;
        }
        
        // Check if deposit account exists
        DepositAccount account = getAccountByIdentificationNo(identificationNo);
        if (account == null) {
            System.out.println("Withdrawal failed: no deposit account is found");
            return null;
        }
        
        // Check if account is Active
        if (!account.isActive()) {
            System.out.printf("Withdrawal failed: Cannot withdraw funds. Account status is %s (must be Active)%n", 
                account.getStatus());
            return null;
        }
        
        // Validate sufficient balance
        if (account.getBalance().compareTo(amount) < 0) {
            System.out.println("Withdrawal failed: insufficient balance");
            return null;
        }
        
        // Debit amount
        BigDecimal oldBalance = account.getBalance();
        BigDecimal newBalance = oldBalance.subtract(amount);
        
        String sql = "UPDATE DEPOSIT_ACCOUNT SET BALANCE = ? WHERE ACCOUNT_ID = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setString(2, account.getAccountId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Withdrawal failed: " + e.getMessage());
            return null;
        }
        
        System.out.printf("Withdrew $%s from account %s%n", amount, account.getAccountId());
        account.setBalance(newBalance);
        return account;
    }
    
    // Freezes or unfreezes
    @Override
    public synchronized DepositAccount updateDepositAccountStatus(String identificationNo, String profilePassword, String action) {
        if (identificationNo == null || identificationNo.trim().isEmpty()) {
            System.out.println("Status update failed: Identification number is required");
            return null;
        }
        
        if (profilePassword == null || profilePassword.trim().isEmpty()) {
            System.out.println("Status update failed: Profile password is required");
            return null;
        }
        
        if (action == null || (!action.equalsIgnoreCase("FREEZE") && !action.equalsIgnoreCase("UNFREEZE"))) {
            System.out.println("Status update failed: Action must be either 'FREEZE' or 'UNFREEZE'");
            return null;
        }
        
        if (!customerService.verifyLogin(identificationNo, profilePassword)) {
            System.out.println("Status update failed: password is incorrect");
            return null;
        }
        
        // Check if deposit account exists
        DepositAccount account = getAccountByIdentificationNo(identificationNo);
        if (account == null) {
            System.out.println("Status update failed: no deposit account is found");
            return null;
        }
        
        // Check if account is Closed
        if (account.isClosed()) {
            System.out.println("Status update failed: Cannot update status. Account is Closed");
            return null;
        }
        
        // Process status update
        String currentStatus = account.getStatus();
        
        if (action.equalsIgnoreCase("FREEZE")) {
            if (account.isActive()) {
                String sql = "UPDATE DEPOSIT_ACCOUNT SET STATUS = ? WHERE ACCOUNT_ID = ?";
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, "Frozen");
                    ps.setString(2, account.getAccountId());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    System.out.println("Account freeze failed: " + e.getMessage());
                    return null;
                }
                System.out.println("Account frozen: " + account.getAccountId());
                account.setStatus("Frozen");
                return account;
            } else if (account.isFrozen()) {
                System.out.println("Status update failed: Account is already Frozen");
                return null;
            } else {
                System.out.printf("Status update failed: Cannot freeze account with status: %s%n", currentStatus);
                return null;
            }
        } else { // UNFREEZE
            if (account.isFrozen()) {
                String sql = "UPDATE DEPOSIT_ACCOUNT SET STATUS = ? WHERE ACCOUNT_ID = ?";
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, "Active");
                    ps.setString(2, account.getAccountId());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    System.out.println("Account unfreeze failed: " + e.getMessage());
                    return null;
                }
                System.out.println("Account unfrozen: " + account.getAccountId());
                account.setStatus("Active");
                return account;
            } else if (account.isActive()) {
                System.out.println("Status update failed: Account is already Active");
                return null;
            } else {
                System.out.printf("Status update failed: Cannot unfreeze account with status: %s%n", currentStatus);
                return null;
            }
        }
    }
}
