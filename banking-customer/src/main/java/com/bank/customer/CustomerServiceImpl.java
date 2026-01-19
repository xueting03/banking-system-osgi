package com.bank.customer;

import com.bank.api.Customer;
import com.bank.api.ICustomerService;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.sql.DataSource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = ICustomerService.class, immediate = true)
public class CustomerServiceImpl implements ICustomerService {

    @Reference
    private DataSource dataSource;

    @Activate
    void activate() {
        try (Connection connection = dataSource.getConnection()) {
            initSchema(connection);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize CustomerServiceImpl", e);
        }
    }

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
            MessageDigest md = MessageDigest.getInstance("SHA-256");
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
        LocalDateTime now = LocalDateTime.now();
        String passwordHash = hashPassword(password);

        String sql = "INSERT INTO CUSTOMER (ID, NAME, EMAIL, IDENTIFICATION_NO, PASSWORD_HASH, STATUS, CREATED_AT) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, name);
            ps.setString(3, email);
            ps.setString(4, null);
            ps.setString(5, passwordHash);
            ps.setString(6, "ACTIVE");
            ps.setTimestamp(7, Timestamp.valueOf(now));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error creating customer", e);
        }

        Customer customer = new Customer(id, name, email);
        customer.setPassword(passwordHash);
        customer.setStatus("ACTIVE");
        customer.setCreatedAt(Timestamp.valueOf(now));
        return customer;
    }

    // Helper method to register identification number
    public void registerIdentificationNo(Customer customer) {
        if (customer == null || customer.getId() == null || customer.getIdentificationNo() == null) {
            return;
        }
        String sql = "UPDATE CUSTOMER SET IDENTIFICATION_NO = ? WHERE ID = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, customer.getIdentificationNo());
            ps.setString(2, customer.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error registering identification number", e);
        }
    }

    @Override
    public Customer updateCustomer(String id, String name, String email) {
        return updateCustomer(id, name, email, null, null, null);
    }

    // Overloaded for partial update and password change
    public Customer updateCustomer(String id, String name, String email, String currentPassword, String newPassword, String status) {
        CustomerRecord record = loadCustomerById(id);
        if (record == null) {
            System.out.println("Update failed: Customer not found (" + id + ")");
            return null;
        }

        // If password change is requested, verify current password
        if (newPassword != null && !newPassword.isEmpty()) {
            if (currentPassword == null || !verifyPassword(currentPassword, record.passwordHash)) {
                System.out.println("Update failed: Incorrect current password for customer (" + id + ")");
                throw new IllegalArgumentException("Current password incorrect");
            }
            if (!isPasswordValid(newPassword)) {
                System.out.println("Update failed: New password does not meet criteria for customer (" + id + ")");
                throw new IllegalArgumentException("New password does not meet criteria");
            }
            record.passwordHash = hashPassword(newPassword);
        }
        if (name != null) record.customer.setName(name);
        if (email != null) record.customer.setEmail(email);
        if (status != null) record.customer.setStatus(status);

        String sql = "UPDATE CUSTOMER SET NAME = ?, EMAIL = ?, STATUS = ?, PASSWORD_HASH = ? WHERE ID = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, record.customer.getName());
            ps.setString(2, record.customer.getEmail());
            ps.setString(3, record.customer.getStatus());
            ps.setString(4, record.passwordHash);
            ps.setString(5, record.customer.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating customer", e);
        }

        return record.customer;
    }

    @Override
    public Customer getCustomer(String idOrIdentification) {
        CustomerRecord byId = loadCustomerById(idOrIdentification);
        if (byId != null) {
            return byId.customer;
        }
        CustomerRecord byIdentification = loadCustomerByIdentification(idOrIdentification);
        return byIdentification == null ? null : byIdentification.customer;
    }

    // Login/verify method
    public boolean verifyLogin(String idOrIdentificationNo, String password) {
        CustomerRecord record = loadCustomerById(idOrIdentificationNo);
        if (record == null) {
            record = loadCustomerByIdentification(idOrIdentificationNo);
        }

        if (record == null) {
            System.out.println("Login failed: Customer not found (" + idOrIdentificationNo + ")");
            return false;
        }
        if (!"ACTIVE".equalsIgnoreCase(record.customer.getStatus())) {
            System.out.println("Login failed: Customer not active (" + idOrIdentificationNo + ")");
            return false;
        }
        boolean success = verifyPassword(password, record.passwordHash);
        if (success) {
            System.out.println("Login successful for customer: " + idOrIdentificationNo);
        } else {
            System.out.println("Login failed: Incorrect password for customer: " + idOrIdentificationNo);
        }
        return success;
    }

    private void initSchema(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS CUSTOMER ("
            + "ID VARCHAR(36) PRIMARY KEY, "
            + "NAME VARCHAR(255) NOT NULL, "
            + "EMAIL VARCHAR(255), "
            + "IDENTIFICATION_NO VARCHAR(255), "
            + "PASSWORD_HASH VARCHAR(255) NOT NULL, "
            + "STATUS VARCHAR(32), "
            + "CREATED_AT TIMESTAMP"
            + ")";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    private CustomerRecord loadCustomerById(String id) {
        String sql = "SELECT ID, NAME, EMAIL, IDENTIFICATION_NO, PASSWORD_HASH, STATUS, CREATED_AT FROM CUSTOMER WHERE ID = ?";
        return loadSingle(sql, id);
    }

    private CustomerRecord loadCustomerByIdentification(String identificationNo) {
        String sql = "SELECT ID, NAME, EMAIL, IDENTIFICATION_NO, PASSWORD_HASH, STATUS, CREATED_AT FROM CUSTOMER WHERE IDENTIFICATION_NO = ?";
        return loadSingle(sql, identificationNo);
    }

    private CustomerRecord loadSingle(String sql, String key) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Customer customer = new Customer(
                        rs.getString("ID"),
                        rs.getString("NAME"),
                        rs.getString("EMAIL")
                    );
                    customer.setIdentificationNo(rs.getString("IDENTIFICATION_NO"));
                    customer.setStatus(rs.getString("STATUS"));
                    customer.setCreatedAt(rs.getTimestamp("CREATED_AT"));
                    String passwordHash = rs.getString("PASSWORD_HASH");
                    return new CustomerRecord(customer, passwordHash);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading customer", e);
        }
        return null;
    }

    private static final class CustomerRecord {
        private final Customer customer;
        private String passwordHash;

        private CustomerRecord(Customer customer, String passwordHash) {
            this.customer = customer;
            this.passwordHash = passwordHash;
        }
    }
}
