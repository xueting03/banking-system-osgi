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
            // Use reflection to call the overloaded createCustomer method with IC
            java.lang.reflect.Method method = customerService.getClass().getMethod(
                "createCustomer", String.class, String.class, String.class, String.class);
            Customer customer = (Customer) method.invoke(customerService, ic, name, email, password);
            if (customer != null) {
                return "Customer created successfully: " + customer.getName() +
                       " (ID: " + customer.getId() + ", IC: " + ic + ", Email: " + customer.getEmail() + ")";
            }
            return "Error: Failed to create customer";
        } catch (Exception e) {
            Throwable cause = e.getCause();
            String msg = (cause != null && cause.getMessage() != null)
                ? cause.getMessage()
                : (e.getMessage() != null ? e.getMessage() : e.toString());
            return "Error creating customer: " + msg;
        }
    }

    /**
     * Update customer details (name, email, password, status)
     * customer:update <id> <name> <email> [<currentPassword> <newPassword> <status>]
     */
    public String update(String id, String name, String email, String currentPassword, String newPassword, String status) {
        try {
            // Use reflection to call the overloaded updateCustomer method
            java.lang.reflect.Method method = customerService.getClass().getMethod(
                "updateCustomer", String.class, String.class, String.class, String.class, String.class, String.class);
            Customer customer = (Customer) method.invoke(customerService, id, name, email, currentPassword, newPassword, status);
            if (customer != null) {
                return "Customer updated: " + customer.getName() +
                       " (ID: " + customer.getId() + ", IC: " + customer.getIdentificationNo() + ", Email: " + customer.getEmail() + ", Status: " + customer.getStatus() + ")";
            }
            return "Customer not found with ID: " + id;
        } catch (Exception e) {
            Throwable cause = e.getCause();
            String msg = (cause != null && cause.getMessage() != null)
                ? cause.getMessage()
                : (e.getMessage() != null ? e.getMessage() : e.toString());
            return "Error updating customer: " + msg;
        }
    }

    /**
     * Customer login/verify credentials
     * customer:login <idOrIC> <password>
     */
    public String login(String idOrIC, String password) {
        try {
            java.lang.reflect.Method method = customerService.getClass().getMethod(
                "verifyLogin", String.class, String.class);
            boolean success = (boolean) method.invoke(customerService, idOrIC, password);
            if (success) {
                return "Login successful for: " + idOrIC;
            } else {
                return "Login failed for: " + idOrIC;
            }
        } catch (Exception e) {
            Throwable cause = e.getCause();
            String msg = (cause != null && cause.getMessage() != null)
                ? cause.getMessage()
                : (e.getMessage() != null ? e.getMessage() : e.toString());
            return "Error during login: " + msg;
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
            Throwable cause = e.getCause();
            String msg = (cause != null && cause.getMessage() != null)
                ? cause.getMessage()
                : (e.getMessage() != null ? e.getMessage() : e.toString());
            return "Error retrieving customer: " + msg;
        }
    }
}
