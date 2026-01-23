package com.bank.customer;

import static org.junit.jupiter.api.Assertions.*;

import com.bank.api.Customer;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CustomerServiceImplTest {
    private CustomerServiceImpl service;
    private DataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:customer-test;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        this.dataSource = ds;

        service = new CustomerServiceImpl();
        injectDataSource(service, dataSource);
        service.activate(); // create schema before exercising methods
        clearCustomerTable();
    }

    private void injectDataSource(CustomerServiceImpl target, DataSource dataSource) throws Exception {
        Field field = CustomerServiceImpl.class.getDeclaredField("dataSource");
        field.setAccessible(true);
        field.set(target, dataSource);
    }

    private void clearCustomerTable() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().executeUpdate("DELETE FROM CUSTOMER");
        }
    }

    @Test
    void createCustomer_rejectsWeakPassword() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createCustomer("Weak User", "weak@example.com", "short"));
        assertTrue(ex.getMessage().contains("Password"), "Should mention password criteria");
    }

    @Test
    void registerIdentificationAndFetchByIdentification() {
        Customer customer = service.createCustomer("Jane Doe", "jane@example.com", "Password1");
        customer.setIdentificationNo("IC123");
        service.registerIdentificationNo(customer);

        Customer found = service.getCustomer("IC123");
        assertNotNull(found);
        assertEquals(customer.getId(), found.getId());
        assertEquals("IC123", found.getIdentificationNo());
    }

    @Test
    void verifyLogin_successAndFailure() {
        Customer customer = service.createCustomer("John Doe", "john@example.com", "Password1");
        customer.setIdentificationNo("ID001");
        service.registerIdentificationNo(customer);

        assertTrue(service.verifyLogin("ID001", "Password1"));
        assertFalse(service.verifyLogin("ID001", "wrongPass"));
    }

    @Test
    void verifyLogin_inactiveCustomerFails() {
        Customer customer = service.createCustomer("Inactive", "inactive@example.com", "Password1");
        customer = service.updateCustomer(customer.getId(), null, null, null, null, "INACTIVE");

        assertFalse(service.verifyLogin(customer.getId(), "Password1"));
    }

    @Test
    void updateCustomer_passwordRulesEnforced() {
        Customer customer = service.createCustomer("Update Me", "update@example.com", "Password1");

        // Wrong current password
        assertThrows(IllegalArgumentException.class,
                () -> service.updateCustomer(customer.getId(), null, null, "badCurrent", "Newpass1", null));

        // Weak new password
        assertThrows(IllegalArgumentException.class,
                () -> service.updateCustomer(customer.getId(), null, null, "Password1", "short", null));

        // Happy path
        Customer updated = service.updateCustomer(customer.getId(), "Updated", "new@example.com", "Password1",
                "Newpass1", null);
        assertEquals("Updated", updated.getName());
        assertEquals("new@example.com", updated.getEmail());
        assertTrue(service.verifyLogin(customer.getId(), "Newpass1"));
    }

    @Test
    void basicCrud_getByIdStillWorks() {
        Customer customer = service.createCustomer("Alice", "alice@example.com", "Password1");
        Customer found = service.getCustomer(customer.getId());
        assertNotNull(found);
        assertEquals("Alice", found.getName());

        Customer updated = service.updateCustomer(customer.getId(), "Alice Smith", "alice.smith@example.com");
        assertEquals("Alice Smith", updated.getName());
        assertEquals("alice.smith@example.com", updated.getEmail());
    }

    @Test
    void registerDuplicateIdentificationNo_shouldNotOverwrite() {
        Customer customer1 = service.createCustomer("User1", "user1@example.com", "Password1");
        customer1.setIdentificationNo("DUPLICATEIC");
        service.registerIdentificationNo(customer1);

        Customer customer2 = service.createCustomer("User2", "user2@example.com", "Password2");
        customer2.setIdentificationNo("DUPLICATEIC");
        service.registerIdentificationNo(customer2);

        // Should still find the first customer by IC
        Customer found = service.getCustomer("DUPLICATEIC");
        assertNotNull(found);
        assertEquals(customer1.getId(), found.getId());
    }

    @Test
    void createCustomer_nullAndEmptyInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createCustomer(null, "email@example.com", "Password1"));
        assertThrows(IllegalArgumentException.class,
                () -> service.createCustomer("", "email@example.com", "Password1"));
        assertThrows(IllegalArgumentException.class, () -> service.createCustomer("Name", null, "Password1"));
        assertThrows(IllegalArgumentException.class, () -> service.createCustomer("Name", "", "Password1"));
        assertThrows(IllegalArgumentException.class, () -> service.createCustomer("Name", "email@example.com", null));
        assertThrows(IllegalArgumentException.class, () -> service.createCustomer("Name", "email@example.com", ""));
    }

    @Test
    void updateCustomer_nullInputsAndStatusEdgeCases() {
        Customer customer = service.createCustomer("Edge", "edge@example.com", "Password1");
        // Null/empty updates should not throw
        assertDoesNotThrow(() -> service.updateCustomer(customer.getId(), null, null));
        assertDoesNotThrow(() -> service.updateCustomer(customer.getId(), "Edge", null));
        assertDoesNotThrow(() -> service.updateCustomer(customer.getId(), null, "edge2@example.com"));

        // Set to INACTIVE, then ACTIVE again
        Customer inactive = service.updateCustomer(customer.getId(), null, null, null, null, "INACTIVE");
        assertEquals("INACTIVE", inactive.getStatus());
        Customer active = service.updateCustomer(customer.getId(), null, null, null, null, "ACTIVE");
        assertEquals("ACTIVE", active.getStatus());
    }

    @Test
    void createCustomer_duplicateIC_shouldFail() {
        Customer c1 = service.createCustomer("DupIC", "dup1@example.com", "Password1");
        c1.setIdentificationNo("DUPIC123");
        service.registerIdentificationNo(c1);

        Customer c2 = service.createCustomer("DupIC2", "dup2@example.com", "Password2");
        c2.setIdentificationNo("DUPIC123");
        // Should not overwrite the first customer
        service.registerIdentificationNo(c2);

        Customer found = service.getCustomer("DUPIC123");
        assertEquals(c1.getId(), found.getId());
    }

    @Test
    void createCustomer_weakPassword_shouldFail() {
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
            service.createCustomer("WeakPass", "weak@example.com", "123")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("password"));
    }

    @Test
    void updateCustomer_emailOnly_shouldSucceed() {
        Customer c = service.createCustomer("EmailOnly", "old@example.com", "Password1");
        Customer updated = service.updateCustomer(c.getId(), "EmailOnly", "new@example.com");
        assertEquals("new@example.com", updated.getEmail());
    }

    @Test
    void login_inactiveCustomer_shouldFail() {
        Customer c = service.createCustomer("Inactive", "inactive@example.com", "Password1");
        service.updateCustomer(c.getId(), "Inactive", "inactive@example.com", "Password1", "Password1", "INACTIVE");
        assertFalse(service.verifyLogin(c.getId(), "Password1"));
    }

    @Test
    void getCustomer_nonexistentId_shouldReturnNull() {
        assertNull(service.getCustomer("nonexistent-id"));
    }
}
