package com.bank.cli;

import com.bank.api.Customer;
import com.bank.api.ICustomerService;

public class CustomerCommands {

    private final ICustomerService customerService;

    public CustomerCommands(ICustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * Create a new customer with IC and password
     * customer:create <ic> <password> <name> <email>
     */
    public String create(String ic, String password, String name, String email) {
        try {
            // Use reflection to call the overloaded createCustomer method with password
            java.lang.reflect.Method method = customerService.getClass().getMethod(
                "createCustomer", String.class, String.class, String.class);
            Customer customer = (Customer) method.invoke(customerService, name, email, password);
            
            if (customer != null) {
                // Set IC number
                customer.setIdentificationNo(ic);
                
                // Register IC number mapping using reflection
                java.lang.reflect.Method registerMethod = customerService.getClass().getMethod(
                    "registerIdentificationNo", Customer.class);
                registerMethod.invoke(customerService, customer);
                
                return "Customer created successfully: " + customer.getName() + 
                       " (ID: " + customer.getId() + ", IC: " + ic + ", Email: " + customer.getEmail() + ")";
            }
            return "Error: Failed to create customer";
        } catch (Exception e) {
            return "Error creating customer: " + e.getMessage();
        }
    }

    /**
     * Get customer details by IC
     * customer:get <ic>
     */
    public String get(String ic) {
        try {
            Customer customer = customerService.getCustomer(ic);
            if (customer != null) {
                return "Customer: " + customer.getName() + 
                       " (ID: " + customer.getId() + 
                       ", IC: " + customer.getIdentificationNo() + 
                       ", Email: " + customer.getEmail() + 
                       ", Status: " + customer.getStatus() + ")";
            }
            return "Customer not found with IC: " + ic;
        } catch (Exception e) {
            return "Error retrieving customer: " + e.getMessage();
        }
    }
}
