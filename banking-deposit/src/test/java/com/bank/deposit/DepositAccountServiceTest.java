package com.bank.deposit;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;

import com.bank.api.Customer;
import com.bank.api.ICustomerService;

public class DepositAccountServiceTest {
    
    private DepositAccountServiceImpl depositService;
    private ICustomerService mockCustomerService;
    
    private static final String CUSTOMER_ID = "CUST12345678";
    private static final String ID_NO = "030119-08-3006";
    private static final String PROFILE_PASSWORD = "alice123";
    private static final String WRONG_PASSWORD = "wrongpass";
    
    @BeforeEach
    public void setUp() {
        mockCustomerService = Mockito.mock(ICustomerService.class);
        depositService = new DepositAccountServiceImpl(mockCustomerService);
        
        // Setup mock customer using available 3-parameter constructor
        Customer customer = new Customer(CUSTOMER_ID, "Alice Johnson", "alice@example.com");
        customer.setIdentificationNo(ID_NO);
        customer.setPassword(PROFILE_PASSWORD);
        when(mockCustomerService.getCustomer(ID_NO)).thenReturn(customer);
        when(mockCustomerService.verifyLogin(ID_NO, PROFILE_PASSWORD)).thenReturn(true);
        when(mockCustomerService.verifyLogin(ID_NO, WRONG_PASSWORD)).thenReturn(false);
    }
    
    @Test
    @DisplayName("Create deposit account - Success")
    public void testCreateDepositAccount_Success() {
        String result = depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        assertTrue(result.startsWith("SUCCESS"));
        assertTrue(result.contains("Deposit account created"));
        assertTrue(result.contains("Initial Balance: $0"));
        assertTrue(result.contains("Status: Active"));
    }
    
    @Test
    @DisplayName("Create deposit account - Success with initial balance")
    public void testCreateDepositAccount_SuccessWithInitialBalance() {
        String result = depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, new BigDecimal("500.00"));
        
        assertTrue(result.startsWith("SUCCESS"));
        assertTrue(result.contains("Deposit account created"));
        assertTrue(result.contains("Initial Balance: $500.00"));
        assertTrue(result.contains("Status: Active"));
    }
    
    @Test
    @DisplayName("Create deposit account - Invalid profile password")
    public void testCreateDepositAccount_InvalidProfilePassword() {
        String result = depositService.createDepositAccount(ID_NO, WRONG_PASSWORD, null);
        
        assertTrue(result.startsWith("ERROR"));
        assertTrue(result.contains("password is incorrect"));
    }
    
    @Test
    @DisplayName("Create deposit account - Already exists")
    public void testCreateDepositAccount_AlreadyExists() {
        // Create first account
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        // Try to create duplicate
        String result = depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, new BigDecimal("100"));
        
        assertTrue(result.startsWith("ERROR"));
        assertTrue(result.contains("deposit account already exists"));
    }
    
    @Test
    @DisplayName("Create deposit account - Missing identification number")
    public void testCreateDepositAccount_MissingIdNo() {
        String result = depositService.createDepositAccount(null, PROFILE_PASSWORD, null);
        
        assertTrue(result.startsWith("ERROR"));
        assertTrue(result.contains("Identification number is required"));
    }
    
    @Test
    @DisplayName("Get deposit account - Success")
    public void testGetDepositAccount_Success() {
        // Create account first
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        String result = depositService.getDepositAccount(ID_NO, PROFILE_PASSWORD);
        
        assertTrue(result.startsWith("SUCCESS"));
        assertTrue(result.contains("Account ID"));
        assertTrue(result.contains("Status: Active"));
        assertTrue(result.contains("Balance: $0"));
    }
    
    @Test
    @DisplayName("Get deposit account - Invalid password")
    public void testGetDepositAccount_InvalidPassword() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        String result = depositService.getDepositAccount(ID_NO, WRONG_PASSWORD);
        
        assertTrue(result.startsWith("ERROR"));
        assertTrue(result.contains("password is incorrect"));
    }
    
    @Test
    @DisplayName("Get deposit account - No account found")
    public void testGetDepositAccount_NoAccountFound() {
        String result = depositService.getDepositAccount(ID_NO, PROFILE_PASSWORD);
        
        assertTrue(result.startsWith("ERROR"));
        assertTrue(result.contains("no deposit account is found"));
    }
    
    @Test
    @DisplayName("Close deposit account - Success")
    public void testCloseDepositAccount_Success() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        String result = depositService.closeDepositAccount(ID_NO, PROFILE_PASSWORD);
        
        assertTrue(result.startsWith("SUCCESS"));
        assertTrue(result.contains("Deposit account closed"));
    }
    
    @Test
    @DisplayName("Close deposit account - Invalid password")
    public void testCloseDepositAccount_InvalidPassword() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        String result = depositService.closeDepositAccount(ID_NO, WRONG_PASSWORD);
        
        assertTrue(result.startsWith("ERROR"));
        assertTrue(result.contains("password is incorrect"));
    }
    
    // ========== Deposit Funds Tests ==========
    
    @Test
    @DisplayName("Deposit funds - Success")
    public void testDepositFunds_Success() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        String result = depositService.depositFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("1000.00"));
        
        assertTrue(result.startsWith("SUCCESS"));
        assertTrue(result.contains("Deposited $1000.00"));
        assertTrue(result.contains("New Balance: $1000.00"));
    }
    
    @Test
    @DisplayName("Deposit funds - Invalid password")
    public void testDepositFunds_InvalidPassword() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        String result = depositService.depositFunds(ID_NO, WRONG_PASSWORD, new BigDecimal("1000"));
        
        assertTrue(result.startsWith("ERROR"));
        assertTrue(result.contains("password is incorrect"));
    }
    
    @Test
    @DisplayName("Deposit funds - Non-positive amount")
    public void testDepositFunds_NonPositiveAmount() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        String result1 = depositService.depositFunds(ID_NO, PROFILE_PASSWORD, BigDecimal.ZERO);
        assertTrue(result1.startsWith("ERROR"));
        assertTrue(result1.contains("must be greater than zero"));
        
        String result2 = depositService.depositFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("-100"));
        assertTrue(result2.startsWith("ERROR"));
        assertTrue(result2.contains("must be greater than zero"));
    }
    
    @Test
    @DisplayName("Deposit funds - Account frozen")
    public void testDepositFunds_AccountFrozen() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        depositService.updateDepositAccountStatus(ID_NO, PROFILE_PASSWORD, "FREEZE");
        
        String result = depositService.depositFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("100"));
        
        assertTrue(result.startsWith("ERROR"));
        assertTrue(result.contains("Cannot deposit funds"));
        assertTrue(result.contains("Frozen"));
    }
    
    @Test
    @DisplayName("Deposit funds - Account closed")
    public void testDepositFunds_AccountClosed() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        depositService.closeDepositAccount(ID_NO, PROFILE_PASSWORD);
        
        String result = depositService.depositFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("100"));
        
        assertTrue(result.startsWith("ERROR"));
        assertTrue(result.contains("Cannot deposit funds"));
        assertTrue(result.contains("Closed"));
    }
    
    @Test
    @DisplayName("Deposit funds - No account found")
    public void testDepositFunds_NoAccountFound() {
        String result = depositService.depositFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("100"));
        
        assertTrue(result.startsWith("ERROR"));
        assertTrue(result.contains("no deposit account is found"));
    }
    
    // ========== Withdraw Funds Tests ==========

    @Test
    @DisplayName("Withdraw funds - Success")
    public void testWithdrawFunds_Success() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        depositService.depositFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("1000.00"));
        
        String result = depositService.withdrawFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("300.00"));
        
        assertTrue(result.startsWith("SUCCESS"));
        assertTrue(result.contains("Withdrew $300.00"));
        assertTrue(result.contains("New Balance: $700.00"));
    }
    
    @Test
    @DisplayName("Withdraw funds - Invalid password")
    public void testWithdrawFunds_InvalidPassword() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        depositService.depositFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("1000"));
        
        String result = depositService.withdrawFunds(ID_NO, WRONG_PASSWORD, new BigDecimal("100"));
        
        assertTrue(result.startsWith("ERROR"));
        assertTrue(result.contains("password is incorrect"));
    }
    
    @Test
    @DisplayName("Withdraw funds - Insufficient balance")
    public void testWithdrawFunds_InsufficientBalance() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        depositService.depositFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("500"));
        
        String result = depositService.withdrawFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("1000"));
        
        assertTrue(result.startsWith("ERROR"));
        assertTrue(result.contains("insufficient balance"));
    }
    
    @Test
    @DisplayName("Withdraw funds - Non-positive amount")
    public void testWithdrawFunds_NonPositiveAmount() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        String result = depositService.withdrawFunds(ID_NO, PROFILE_PASSWORD, BigDecimal.ZERO);
        
        assertTrue(result.startsWith("ERROR"));
        assertTrue(result.contains("must be greater than zero"));
    }
    
    @Test
    @DisplayName("Withdraw funds - Account frozen")
    public void testWithdrawFunds_AccountFrozen() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        depositService.depositFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("1000"));
        depositService.updateDepositAccountStatus(ID_NO, PROFILE_PASSWORD, "FREEZE");
        
        String result = depositService.withdrawFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("100"));
        
        assertTrue(result.startsWith("ERROR"));
        assertTrue(result.contains("Cannot withdraw funds"));
        assertTrue(result.contains("Frozen"));
    }
    
    @Test
    @DisplayName("Withdraw funds - Account closed")
    public void testWithdrawFunds_AccountClosed() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        depositService.depositFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("1000"));
        depositService.closeDepositAccount(ID_NO, PROFILE_PASSWORD);
        
        String result = depositService.withdrawFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("100"));
        
        assertTrue(result.startsWith("ERROR"));
        assertTrue(result.contains("Cannot withdraw funds"));
        assertTrue(result.contains("Closed"));
    }
    
    @Test
    @DisplayName("Withdraw funds - No account found")
    public void testWithdrawFunds_NoAccountFound() {
        String result = depositService.withdrawFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("100"));
        
        assertTrue(result.startsWith("ERROR"));
        assertTrue(result.contains("no deposit account is found"));
    }

    // ========== Update Status Tests ==========
    
    @Test
    @DisplayName("Freeze account - Success")
    public void testUpdateStatus_FreezeSuccess() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        String result = depositService.updateDepositAccountStatus(ID_NO, PROFILE_PASSWORD, "FREEZE");
        
        assertTrue(result.startsWith("SUCCESS"));
        assertTrue(result.contains("Account frozen"));
        assertTrue(result.contains("New Status: Frozen"));
    }
    
    @Test
    @DisplayName("Unfreeze account - Success")
    public void testUpdateStatus_UnfreezeSuccess() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        depositService.updateDepositAccountStatus(ID_NO, PROFILE_PASSWORD, "FREEZE");
        
        String result = depositService.updateDepositAccountStatus(ID_NO, PROFILE_PASSWORD, "UNFREEZE");
        
        assertTrue(result.startsWith("SUCCESS"));
        assertTrue(result.contains("Account unfrozen"));
        assertTrue(result.contains("New Status: Active"));
    }
    
    @Test
    @DisplayName("Freeze account - Invalid password")
    public void testUpdateStatus_InvalidPassword() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        String result = depositService.updateDepositAccountStatus(ID_NO, WRONG_PASSWORD, "FREEZE");
        
        assertTrue(result.startsWith("ERROR"));
        assertTrue(result.contains("password is incorrect"));
    }
    
    @Test
    @DisplayName("Freeze account - Already frozen")
    public void testUpdateStatus_AlreadyFrozen() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        depositService.updateDepositAccountStatus(ID_NO, PROFILE_PASSWORD, "FREEZE");
        
        String result = depositService.updateDepositAccountStatus(ID_NO, PROFILE_PASSWORD, "FREEZE");
        
        assertTrue(result.startsWith("ERROR"));
        assertTrue(result.contains("already Frozen"));
    }
    
    @Test
    @DisplayName("Unfreeze account - Already active")
    public void testUpdateStatus_AlreadyActive() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        String result = depositService.updateDepositAccountStatus(ID_NO, PROFILE_PASSWORD, "UNFREEZE");
        
        assertTrue(result.startsWith("ERROR"));
        assertTrue(result.contains("already Active"));
    }
    
    @Test
    @DisplayName("Update status - Account closed")
    public void testUpdateStatus_AccountClosed() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        depositService.closeDepositAccount(ID_NO, PROFILE_PASSWORD);
        
        String result = depositService.updateDepositAccountStatus(ID_NO, PROFILE_PASSWORD, "FREEZE");
        
        assertTrue(result.startsWith("ERROR"));
        assertTrue(result.contains("Account is Closed"));
    }
    
    @Test
    @DisplayName("Update status - Invalid action")
    public void testUpdateStatus_InvalidAction() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        String result = depositService.updateDepositAccountStatus(ID_NO, PROFILE_PASSWORD, "INVALID");
        
        assertTrue(result.startsWith("ERROR"));
        assertTrue(result.contains("must be either 'FREEZE' or 'UNFREEZE'"));
    }
    
    @Test
    @DisplayName("Complete workflow test")
    public void testCompleteWorkflow() {
        String createResult = depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        assertTrue(createResult.startsWith("SUCCESS"));
        
        String depositResult = depositService.depositFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("1000"));
        assertTrue(depositResult.startsWith("SUCCESS"));
        
        String withdrawResult = depositService.withdrawFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("300"));
        assertTrue(withdrawResult.startsWith("SUCCESS"));
        assertTrue(withdrawResult.contains("New Balance: $700"));
        
        String freezeResult = depositService.updateDepositAccountStatus(ID_NO, PROFILE_PASSWORD, "FREEZE");
        assertTrue(freezeResult.startsWith("SUCCESS"));
        
        String frozenDepositResult = depositService.depositFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("100"));
        assertTrue(frozenDepositResult.startsWith("ERROR"));
        
        String unfreezeResult = depositService.updateDepositAccountStatus(ID_NO, PROFILE_PASSWORD, "UNFREEZE");
        assertTrue(unfreezeResult.startsWith("SUCCESS"));
        
        String getResult = depositService.getDepositAccount(ID_NO, PROFILE_PASSWORD);
        assertTrue(getResult.startsWith("SUCCESS"));
        assertTrue(getResult.contains("Status: Active"));
        assertTrue(getResult.contains("Balance: $700"));
    }
}
