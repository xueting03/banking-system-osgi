package com.bank.customer;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.bank.api.Customer;
import com.bank.api.ICustomerService;

public class Activator implements BundleActivator {
    private ServiceRegistration<?> registration;

    @Override
    public void start(BundleContext context) throws Exception {
        CustomerServiceImpl customerService = new CustomerServiceImpl();
        
        // Pre-populate demo customers for testing
        initializeDemoCustomers(customerService);
        
        registration = context.registerService(
            ICustomerService.class.getName(),
            customerService,
            null
        );
        System.out.println("=== Customer Service Bundle Started ===");
        System.out.println("Customer Service registered successfully");
    }
    
    private void initializeDemoCustomers(CustomerServiceImpl customerService) {
        System.out.println("\n--- Initializing Demo Customers ---");
        
        // Create customers with specific IDs for demo
        Customer alice = customerService.createCustomer("Alice Johnson", "alice@example.com", "alice123");
        alice.setIdentificationNo("030119-08-3006");
        customerService.registerIdentificationNo(alice);
        
        Customer bob = customerService.createCustomer("Bob Smith", "bob@example.com", "bob456789");
        bob.setIdentificationNo("030220-08-3006");
        customerService.registerIdentificationNo(bob);
        
        Customer charlie = customerService.createCustomer("Charlie Brown", "charlie@example.com", "charlie99");
        charlie.setIdentificationNo("030331-08-3006");
        customerService.registerIdentificationNo(charlie);
        
        System.out.println("Demo customers initialized:");
        System.out.println("  - 030119-08-3006 : Alice Johnson (password: alice123)");
        System.out.println("  - 030220-08-3006 : Bob Smith (password: bob456789)");
        System.out.println("  - 030331-08-3006 : Charlie Brown (password: charlie99)");
        System.out.println();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        System.out.println("=== Customer Service Bundle Stopped ===");
        if (registration != null) {
            registration.unregister();
        }
    }
}