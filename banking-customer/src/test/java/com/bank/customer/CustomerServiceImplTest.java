package com.bank.customer;

import com.bank.api.Customer;
import java.lang.reflect.Field;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CustomerServiceImplTest {
    private CustomerServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:customer-test;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");

        service = new CustomerServiceImpl();
        injectDataSource(service, ds);
        service.activate(); // create schema before exercising methods
    }

    private void injectDataSource(CustomerServiceImpl target, DataSource dataSource) throws Exception {
        Field field = CustomerServiceImpl.class.getDeclaredField("dataSource");
        field.setAccessible(true);
        field.set(target, dataSource);
    }

    @Test
    public void testUpdateCustomer() {
        Customer customer = service.createCustomer("Jane Doe", "jane@example.com");
        assertNotNull(customer);
        Customer updated = service.updateCustomer(customer.getId(), "Jane Smith", "jane.smith@example.com");
        assertNotNull(updated);
        assertEquals("Jane Smith", updated.getName());
        assertEquals("jane.smith@example.com", updated.getEmail());
    }

    @Test
    public void testGetCustomer() {
        Customer customer = service.createCustomer("Alice", "alice@example.com");
        assertNotNull(customer);
        Customer found = service.getCustomer(customer.getId());
        assertNotNull(found);
        assertEquals("Alice", found.getName());
        assertEquals("alice@example.com", found.getEmail());
    }
    @Test
    public void testCreateCustomer() {
        Customer customer = service.createCustomer("John Doe", "john@example.com");
        assertNotNull(customer);
        assertEquals("John Doe", customer.getName());
        assertEquals("john@example.com", customer.getEmail());
    }

}
