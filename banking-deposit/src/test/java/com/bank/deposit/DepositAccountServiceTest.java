package com.bank.deposit;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;

import com.bank.api.Customer;
import com.bank.api.DepositAccount;
import com.bank.api.ICustomerService;

public class DepositAccountServiceTest {
    
    private DepositAccountServiceImpl depositService;
    private ICustomerService mockCustomerService;
    private DataSource mockDataSource;
    private Connection testConnection;
    
    private static final String CUSTOMER_ID = "CUST12345678";
    private static final String ID_NO = "030119-08-3006";
    private static final String PROFILE_PASSWORD = "alice123";
    private static final String WRONG_PASSWORD = "wrongpass";
    
    @BeforeEach
    public void setUp() throws Exception {
        String dbUrl = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
        mockDataSource = Mockito.mock(DataSource.class);
        when(mockDataSource.getConnection()).thenAnswer(invocation -> 
            DriverManager.getConnection(dbUrl, "sa", "")
        );
        
        // Mock CustomerService
        mockCustomerService = Mockito.mock(ICustomerService.class);
        
        Customer customer = new Customer(CUSTOMER_ID, "Alice Johnson", "alice@example.com");
        customer.setIdentificationNo(ID_NO);
        customer.setPassword(PROFILE_PASSWORD);
        when(mockCustomerService.getCustomer(ID_NO)).thenReturn(customer);
        when(mockCustomerService.verifyLogin(ID_NO, PROFILE_PASSWORD)).thenReturn(true);
        when(mockCustomerService.verifyLogin(ID_NO, WRONG_PASSWORD)).thenReturn(false);
        
        // Create service instance and inject dependencies using reflection
        depositService = new DepositAccountServiceImpl();
        injectDependency(depositService, "customerService", mockCustomerService);
        injectDependency(depositService, "dataSource", mockDataSource);
        
        depositService.activate();
        
        testConnection = DriverManager.getConnection(dbUrl, "sa", "");
    }
    
    @AfterEach
    public void tearDown() throws SQLException {
        if (testConnection != null && !testConnection.isClosed()) {
            // Clean up database
            testConnection.createStatement().execute("DROP TABLE IF EXISTS DEPOSIT_ACCOUNT");
            testConnection.close();
        }
    }
    

    private void injectDependency(Object target, String fieldName, Object dependency) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, dependency);
    }
    
    @Test
    @DisplayName("Create deposit account - Success")
    public void testCreateDepositAccount_Success() {
        DepositAccount result = depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        assertNotNull(result);
        assertNotNull(result.getAccountId());
        assertEquals("Active", result.getStatus());
        assertEquals(new BigDecimal("0.00"), result.getBalance());
    }
    
    @Test
    @DisplayName("Create deposit account - Success with initial balance")
    public void testCreateDepositAccount_SuccessWithInitialBalance() {
        DepositAccount result = depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, new BigDecimal("500.00"));
        
        assertNotNull(result);
        assertNotNull(result.getAccountId());
        assertEquals("Active", result.getStatus());
        assertEquals(new BigDecimal("500.00"), result.getBalance());
    }
    
    @Test
    @DisplayName("Create deposit account - Invalid profile password")
    public void testCreateDepositAccount_InvalidProfilePassword() {
        DepositAccount result = depositService.createDepositAccount(ID_NO, WRONG_PASSWORD, null);
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("Create deposit account - Already exists")
    public void testCreateDepositAccount_AlreadyExists() {
        // Create first account
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        // Try to create duplicate
        DepositAccount result = depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, new BigDecimal("100"));
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("Create deposit account - Missing identification number")
    public void testCreateDepositAccount_MissingIdNo() {
        DepositAccount result = depositService.createDepositAccount(null, PROFILE_PASSWORD, null);
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("Get deposit account - Success")
    public void testGetDepositAccount_Success() {
        // Create account first
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        DepositAccount result = depositService.getDepositAccount(ID_NO, PROFILE_PASSWORD);
        
        assertNotNull(result);
        assertNotNull(result.getAccountId());
        assertEquals("Active", result.getStatus());
        assertEquals(new BigDecimal("0.00"), result.getBalance());
    }
    
    @Test
    @DisplayName("Get deposit account - Invalid password")
    public void testGetDepositAccount_InvalidPassword() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        DepositAccount result = depositService.getDepositAccount(ID_NO, WRONG_PASSWORD);
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("Get deposit account - No account found")
    public void testGetDepositAccount_NoAccountFound() {
        DepositAccount result = depositService.getDepositAccount(ID_NO, PROFILE_PASSWORD);
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("Close deposit account - Success")
    public void testCloseDepositAccount_Success() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        DepositAccount result = depositService.closeDepositAccount(ID_NO, PROFILE_PASSWORD);
        
        assertNotNull(result);
        assertEquals("Closed", result.getStatus());
    }
    
    @Test
    @DisplayName("Close deposit account - Invalid password")
    public void testCloseDepositAccount_InvalidPassword() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        DepositAccount result = depositService.closeDepositAccount(ID_NO, WRONG_PASSWORD);
        
        assertNull(result);
    }
    
    // Deposit Funds Tests
    
    @Test
    @DisplayName("Deposit funds - Success")
    public void testDepositFunds_Success() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        DepositAccount result = depositService.depositFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("1000.00"));
        
        assertNotNull(result);
        assertEquals(new BigDecimal("1000.00"), result.getBalance());
        assertEquals("Active", result.getStatus());
    }
    
    @Test
    @DisplayName("Deposit funds - Invalid password")
    public void testDepositFunds_InvalidPassword() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        DepositAccount result = depositService.depositFunds(ID_NO, WRONG_PASSWORD, new BigDecimal("1000"));
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("Deposit funds - Non-positive amount")
    public void testDepositFunds_NonPositiveAmount() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        DepositAccount result1 = depositService.depositFunds(ID_NO, PROFILE_PASSWORD, BigDecimal.ZERO);
        assertNull(result1);
        
        DepositAccount result2 = depositService.depositFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("-100"));
        assertNull(result2);
    }
    
    @Test
    @DisplayName("Deposit funds - Account frozen")
    public void testDepositFunds_AccountFrozen() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        depositService.updateDepositAccountStatus(ID_NO, PROFILE_PASSWORD, "FREEZE");
        
        DepositAccount result = depositService.depositFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("100"));
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("Deposit funds - Account closed")
    public void testDepositFunds_AccountClosed() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        depositService.closeDepositAccount(ID_NO, PROFILE_PASSWORD);
        
        DepositAccount result = depositService.depositFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("100"));
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("Deposit funds - No account found")
    public void testDepositFunds_NoAccountFound() {
        DepositAccount result = depositService.depositFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("100"));
        
        assertNull(result);
    }
    
    // Withdraw Funds Tests

    @Test
    @DisplayName("Withdraw funds - Success")
    public void testWithdrawFunds_Success() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        depositService.depositFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("1000.00"));
        
        DepositAccount result = depositService.withdrawFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("300.00"));
        
        assertNotNull(result);
        assertEquals(new BigDecimal("700.00"), result.getBalance());
        assertEquals("Active", result.getStatus());
    }
    
    @Test
    @DisplayName("Withdraw funds - Invalid password")
    public void testWithdrawFunds_InvalidPassword() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        depositService.depositFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("1000"));
        
        DepositAccount result = depositService.withdrawFunds(ID_NO, WRONG_PASSWORD, new BigDecimal("100"));
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("Withdraw funds - Insufficient balance")
    public void testWithdrawFunds_InsufficientBalance() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        depositService.depositFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("500"));
        
        DepositAccount result = depositService.withdrawFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("1000"));
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("Withdraw funds - Non-positive amount")
    public void testWithdrawFunds_NonPositiveAmount() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        DepositAccount result = depositService.withdrawFunds(ID_NO, PROFILE_PASSWORD, BigDecimal.ZERO);
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("Withdraw funds - Account frozen")
    public void testWithdrawFunds_AccountFrozen() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        depositService.depositFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("1000"));
        depositService.updateDepositAccountStatus(ID_NO, PROFILE_PASSWORD, "FREEZE");
        
        DepositAccount result = depositService.withdrawFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("100"));
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("Withdraw funds - Account closed")
    public void testWithdrawFunds_AccountClosed() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        depositService.depositFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("1000"));
        depositService.closeDepositAccount(ID_NO, PROFILE_PASSWORD);
        
        DepositAccount result = depositService.withdrawFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("100"));
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("Withdraw funds - No account found")
    public void testWithdrawFunds_NoAccountFound() {
        DepositAccount result = depositService.withdrawFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("100"));
        
        assertNull(result);
    }

    // Update Status Tests
    
    @Test
    @DisplayName("Freeze account - Success")
    public void testUpdateStatus_FreezeSuccess() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        DepositAccount result = depositService.updateDepositAccountStatus(ID_NO, PROFILE_PASSWORD, "FREEZE");
        
        assertNotNull(result);
        assertEquals("Frozen", result.getStatus());
    }
    
    @Test
    @DisplayName("Unfreeze account - Success")
    public void testUpdateStatus_UnfreezeSuccess() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        depositService.updateDepositAccountStatus(ID_NO, PROFILE_PASSWORD, "FREEZE");
        
        DepositAccount result = depositService.updateDepositAccountStatus(ID_NO, PROFILE_PASSWORD, "UNFREEZE");
        
        assertNotNull(result);
        assertEquals("Active", result.getStatus());
    }
    
    @Test
    @DisplayName("Freeze account - Invalid password")
    public void testUpdateStatus_InvalidPassword() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        DepositAccount result = depositService.updateDepositAccountStatus(ID_NO, WRONG_PASSWORD, "FREEZE");
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("Freeze account - Already frozen")
    public void testUpdateStatus_AlreadyFrozen() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        depositService.updateDepositAccountStatus(ID_NO, PROFILE_PASSWORD, "FREEZE");
        
        DepositAccount result = depositService.updateDepositAccountStatus(ID_NO, PROFILE_PASSWORD, "FREEZE");
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("Unfreeze account - Already active")
    public void testUpdateStatus_AlreadyActive() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        DepositAccount result = depositService.updateDepositAccountStatus(ID_NO, PROFILE_PASSWORD, "UNFREEZE");
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("Update status - Account closed")
    public void testUpdateStatus_AccountClosed() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        depositService.closeDepositAccount(ID_NO, PROFILE_PASSWORD);
        
        DepositAccount result = depositService.updateDepositAccountStatus(ID_NO, PROFILE_PASSWORD, "FREEZE");
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("Update status - Invalid action")
    public void testUpdateStatus_InvalidAction() {
        depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        
        DepositAccount result = depositService.updateDepositAccountStatus(ID_NO, PROFILE_PASSWORD, "INVALID");
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("Complete workflow test")
    public void testCompleteWorkflow() {
        DepositAccount createResult = depositService.createDepositAccount(ID_NO, PROFILE_PASSWORD, null);
        assertNotNull(createResult);
        assertEquals("Active", createResult.getStatus());
        
        DepositAccount depositResult = depositService.depositFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("1000"));
        assertNotNull(depositResult);
        assertEquals(new BigDecimal("1000.00"), depositResult.getBalance());
        
        DepositAccount withdrawResult = depositService.withdrawFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("300"));
        assertNotNull(withdrawResult);
        assertEquals(new BigDecimal("700.00"), withdrawResult.getBalance());
        
        DepositAccount freezeResult = depositService.updateDepositAccountStatus(ID_NO, PROFILE_PASSWORD, "FREEZE");
        assertNotNull(freezeResult);
        assertEquals("Frozen", freezeResult.getStatus());
        
        DepositAccount frozenDepositResult = depositService.depositFunds(ID_NO, PROFILE_PASSWORD, new BigDecimal("100"));
        assertNull(frozenDepositResult);
        
        DepositAccount unfreezeResult = depositService.updateDepositAccountStatus(ID_NO, PROFILE_PASSWORD, "UNFREEZE");
        assertNotNull(unfreezeResult);
        assertEquals("Active", unfreezeResult.getStatus());
        
        DepositAccount getResult = depositService.getDepositAccount(ID_NO, PROFILE_PASSWORD);
        assertNotNull(getResult);
        assertEquals("Active", getResult.getStatus());
        assertEquals(new BigDecimal("700.00"), getResult.getBalance());
    }
}
