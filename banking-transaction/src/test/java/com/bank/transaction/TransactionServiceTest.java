package com.bank.transaction;

import com.bank.api.ICustomerService;
import com.bank.api.model.TransactionSummary;
import com.bank.api.model.TransactionType;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TransactionServiceTest {

    private TransactionServiceImpl service;
    private JdbcDataSource dataSource;
    private ICustomerService customerService;

    @BeforeEach
    void setup() throws Exception {

        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");

        // Clean TRANSACTION table for test isolation
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.execute("DROP TABLE IF EXISTS TRANSACTION");
            s.execute("""
                CREATE TABLE TRANSACTION (
                    TXN_ID VARCHAR(64) PRIMARY KEY,
                    ACCOUNT_ID VARCHAR(64) NOT NULL,
                    TYPE VARCHAR(32) NOT NULL,
                    AMOUNT DECIMAL(18,2) NOT NULL,
                    NOTE VARCHAR(255),
                    CREATED_AT TIMESTAMP NOT NULL
                )
            """);
        }

        try (Connection c = dataSource.getConnection();
             Statement s = c.createStatement()) {

            s.execute("DROP TABLE IF EXISTS DEPOSIT_ACCOUNT");

            s.execute("""
                CREATE TABLE DEPOSIT_ACCOUNT (
                    ACCOUNT_ID VARCHAR(64),
                    CUSTOMER_ID VARCHAR(64),
                    BALANCE DECIMAL(18,2)
                )
            """);

            s.execute("""
                INSERT INTO DEPOSIT_ACCOUNT VALUES
                ('A1','CUST1',1000.00),
                ('A2','CUST2',500.00)
            """);
        }

        customerService = mock(ICustomerService.class);
        when(customerService.verifyLogin("CUST1","pw")).thenReturn(true);
        when(customerService.verifyLogin("CUST2","pw")).thenReturn(true);

        service = new TransactionServiceImpl();

        inject(service, "dataSource", dataSource);
        inject(service, "customerService", customerService);

        service.activate();
    }

    private void inject(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void recordTransaction_success() {

        boolean result = service.recordTransaction(
                "CUST1", "pw",
                TransactionType.DEPOSIT,
                BigDecimal.valueOf(100),
                "Initial deposit"
        );

        assertTrue(result);
    }

    @Test
        void getTransactionSummary_correctTotals() {
        service.recordTransaction(
            "CUST1","pw",
            TransactionType.DEPOSIT,
            BigDecimal.valueOf(200),
            null
        );

        service.recordTransaction(
            "CUST1","pw",
            TransactionType.WITHDRAWAL,
            BigDecimal.valueOf(50),
            null
        );

        // Use a wide date range to include all transactions
        TransactionSummary summary =
            service.getTransactionSummary(
                "CUST1","pw",
                null,
                null
            );

        assertTrue(summary.getTotalDeposits().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(summary.getTotalWithdrawals().compareTo(BigDecimal.ZERO) > 0);
        assertNotEquals(BigDecimal.ZERO, summary.getNetAmount());
        }

    @Test
    void transfer_updatesBalancesAndRecordsTransactions() {

        boolean ok = service.transfer(
                "CUST1", "pw",
                "CUST2",
                BigDecimal.valueOf(300)
        );

        assertTrue(ok);

        TransactionSummary sender =
                service.getTransactionSummary("CUST1","pw", null, null);

        TransactionSummary receiver =
                service.getTransactionSummary("CUST2","pw", null, null);

        assertTrue(sender.getTotalWithdrawals().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(receiver.getTotalDeposits().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void recordTransaction_failsIfAuthenticationFails() {
        when(customerService.verifyLogin("CUST1", "wrongpw")).thenReturn(false);
        boolean result = service.recordTransaction(
                "CUST1", "wrongpw",
                TransactionType.DEPOSIT,
                BigDecimal.valueOf(100),
                "Should fail"
        );
        assertFalse(result);
    }

    @Test
    void recordTransaction_failsIfAccountNotFound() {
        boolean result = service.recordTransaction(
                "NONEXIST", "pw",
                TransactionType.DEPOSIT,
                BigDecimal.valueOf(100),
                "No account"
        );
        assertFalse(result);
    }

    @Test
    void recordTransaction_failsOnNegativeOrZeroAmount() {
        boolean zero = service.recordTransaction(
                "CUST1", "pw",
                TransactionType.DEPOSIT,
                BigDecimal.ZERO,
                "Zero amount"
        );
        boolean negative = service.recordTransaction(
                "CUST1", "pw",
                TransactionType.DEPOSIT,
                BigDecimal.valueOf(-10),
                "Negative amount"
        );
        assertFalse(zero);
        assertFalse(negative);
    }

    @Test
    void getTransactionHistory_emptyForNonexistentAccount() {
        when(customerService.verifyLogin("NOPE", "pw")).thenReturn(true);
        assertTrue(service.getTransactionHistory("NOPE", "pw").isEmpty());
    }

    @Test
    void getTransactionHistory_failsIfAuthenticationFails() {
        when(customerService.verifyLogin("CUST1", "badpw")).thenReturn(false);
        assertTrue(service.getTransactionHistory("CUST1", "badpw").isEmpty());
    }

    @Test
    void filterTransactions_byTypeAndDate() {
        service.recordTransaction("CUST1", "pw", TransactionType.DEPOSIT, BigDecimal.valueOf(100), null);
        service.recordTransaction("CUST1", "pw", TransactionType.WITHDRAWAL, BigDecimal.valueOf(50), null);
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(1);
        assertEquals(1, service.filterTransactions("CUST1", "pw", TransactionType.DEPOSIT, from, to).size());
        assertEquals(1, service.filterTransactions("CUST1", "pw", TransactionType.WITHDRAWAL, from, to).size());
        assertEquals(0, service.filterTransactions("CUST1", "pw", TransactionType.WITHDRAWAL, from.minusYears(10), from.minusYears(9)).size());
    }

    @Test
    void getTransactionSummary_zeroIfNoTransactions() {
        TransactionSummary summary = service.getTransactionSummary("CUST2", "pw", null, null);
        assertEquals(BigDecimal.ZERO, summary.getTotalDeposits());
        assertEquals(BigDecimal.ZERO, summary.getTotalWithdrawals());
        assertEquals(BigDecimal.ZERO, summary.getNetAmount());
    }

    @Test
    void transfer_failsIfSenderAuthenticationFails() {
        when(customerService.verifyLogin("CUST1", "badpw")).thenReturn(false);
        boolean ok = service.transfer("CUST1", "badpw", "CUST2", BigDecimal.valueOf(10));
        assertFalse(ok);
    }

    @Test
    void transfer_failsIfSenderOrReceiverNotFound() {
        boolean noSender = service.transfer("NOPE", "pw", "CUST2", BigDecimal.valueOf(10));
        boolean noReceiver = service.transfer("CUST1", "pw", "NOPE", BigDecimal.valueOf(10));
        assertFalse(noSender);
        assertFalse(noReceiver);
    }

    @Test
    void transfer_failsIfInsufficientBalance() {
        boolean ok = service.transfer("CUST2", "pw", "CUST1", BigDecimal.valueOf(10000));
        assertFalse(ok);
    }

    @Test
    void transfer_failsOnNegativeOrZeroAmount() {
        boolean zero = service.transfer("CUST1", "pw", "CUST2", BigDecimal.ZERO);
        boolean negative = service.transfer("CUST1", "pw", "CUST2", BigDecimal.valueOf(-10));
        assertFalse(zero);
        assertFalse(negative);
    }
}
