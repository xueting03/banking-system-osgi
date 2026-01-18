package com.bank.customer;

import com.bank.api.Customer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CustomerServiceImplTest {
        @Test
        public void testUpdateCustomer() {
            CustomerServiceImpl service = new CustomerServiceImpl();
            Customer customer = service.createCustomer("Jane Doe", "jane@example.com");
            assertNotNull(customer);
            Customer updated = service.updateCustomer(customer.getId(), "Jane Smith", "jane.smith@example.com");
            assertNotNull(updated);
            assertEquals("Jane Smith", updated.getName());
            assertEquals("jane.smith@example.com", updated.getEmail());
        }

        @Test
        public void testGetCustomer() {
            CustomerServiceImpl service = new CustomerServiceImpl();
            Customer customer = service.createCustomer("Alice", "alice@example.com");
            assertNotNull(customer);
            Customer found = service.getCustomer(customer.getId());
            assertNotNull(found);
            assertEquals("Alice", found.getName());
            assertEquals("alice@example.com", found.getEmail());
        }
    @Test
    public void testCreateCustomer() {
        CustomerServiceImpl service = new CustomerServiceImpl();
        Customer customer = service.createCustomer("John Doe", "john@example.com");
        assertNotNull(customer);
        assertEquals("John Doe", customer.getName());
        assertEquals("john@example.com", customer.getEmail());
    }

}
