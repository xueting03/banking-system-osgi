package com.bank.customer;

import java.util.*;
import com.bank.api.ICustomerService;
import com.bank.api.Customer;

public class CustomerServiceImpl implements ICustomerService {
    private final Map<String, Customer> customers = new HashMap<>();

    // Password validation: at least 8 chars, contains digit, contains letter
    private boolean isPasswordValid(String password) {
        if (password == null) return false;
        return password.length() >= 8 &&
               password.matches(".*\\d.*") &&
               password.matches(".*[a-zA-Z].*");
    }

    // Hash password using SHA-256
    private String hashPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    // Verify password
    private boolean verifyPassword(String rawPassword, String storedHashedPassword) {
        return hashPassword(rawPassword).equals(storedHashedPassword);
    }

    @Override
    public Customer createCustomer(String name, String email) {
        return createCustomer(name, email, "changeme123"); // default password for demo
    }

    // Overloaded for password and status
    public Customer createCustomer(String name, String email, String password) {
        if (!isPasswordValid(password)) {
            throw new IllegalArgumentException("Password does not meet criteria: at least 8 characters, contain a digit and a letter.");
        }
        String id = UUID.randomUUID().toString();
        Customer customer = new Customer(id, name, email);
        // Add password and status fields
        customer.setPassword(hashPassword(password));
        customer.setStatus("ACTIVE");
        customers.put(id, customer);
        System.out.println("Customer created: " + name + " (" + email + ")");
        return customer;
    }

    @Override
    public Customer updateCustomer(String id, String name, String email) {
        return updateCustomer(id, name, email, null, null, null);
    }

    // Overloaded for partial update and password change
    public Customer updateCustomer(String id, String name, String email, String currentPassword, String newPassword, String status) {
        Customer customer = customers.get(id);
        if (customer == null) {
            System.out.println("Update failed: Customer not found (" + id + ")");
            return null;
        }

        // If password change is requested, verify current password
        if (newPassword != null && !newPassword.isEmpty()) {
            if (currentPassword == null || !verifyPassword(currentPassword, customer.getPassword())) {
                System.out.println("Update failed: Incorrect current password for customer (" + id + ")");
                throw new IllegalArgumentException("Current password incorrect");
            }
            if (!isPasswordValid(newPassword)) {
                System.out.println("Update failed: New password does not meet criteria for customer (" + id + ")");
                throw new IllegalArgumentException("New password does not meet criteria");
            }
            customer.setPassword(hashPassword(newPassword));
        }
        if (name != null) customer.setName(name);
        if (email != null) customer.setEmail(email);
        if (status != null) customer.setStatus(status);
        System.out.println("Customer updated: " + customer.getName() + " (" + customer.getEmail() + ")");
        return customer;
    }

    @Override
    public Customer getCustomer(String id) {
        return customers.get(id);
    }


    // Login/verify method
    public boolean verifyLogin(String id, String password) {
        Customer customer = customers.get(id);
        if (customer == null) {
            System.out.println("Login failed: Customer not found (" + id + ")");
            return false;
        }
        if (!"ACTIVE".equalsIgnoreCase(customer.getStatus())) {
            System.out.println("Login failed: Customer not active (" + id + ")");
            return false;
        }
        boolean success = verifyPassword(password, customer.getPassword());
        if (success) {
            System.out.println("Login successful for customer: " + id);
        } else {
            System.out.println("Login failed: Incorrect password for customer: " + id);
        }
        return success;
    }
}