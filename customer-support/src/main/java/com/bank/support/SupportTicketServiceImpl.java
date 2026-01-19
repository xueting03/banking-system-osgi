package com.bank.support;

import com.bank.api.ICustomerService;
import com.bank.api.ISupportTicketService;
import com.bank.api.SupportTicket;
import com.bank.api.TicketStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = ISupportTicketService.class, immediate = true)
public class SupportTicketServiceImpl implements ISupportTicketService {

    @Reference
    private ICustomerService customerService;

    @Reference
    private DataSource dataSource;

    @Activate
    void activate() {
        try (Connection connection = dataSource.getConnection()) {
            initSchema(connection);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize SupportTicketServiceImpl", e);
        }
    }

    @Override
    public SupportTicket createTicket(String customerIdNumber, String authPassword, String title, String description) {
        if (isBlank(customerIdNumber) || isBlank(authPassword) || isBlank(description)) {
            System.out.println("Ticket creation failed: customer id number, password, and description are required.");
            return null;
        }

        if (!customerService.verifyLogin(customerIdNumber, authPassword)) {
            System.out.println("Ticket creation failed: invalid customer credentials for " + customerIdNumber);
            return null;
        }

        var customer = customerService.getCustomer(customerIdNumber);
        if (customer == null) {
            System.out.println("Ticket creation failed: customer profile not found for " + customerIdNumber);
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        String id = UUID.randomUUID().toString();
        String ticketTitle = coalesce(title, "General Inquiry");

        String sql = "INSERT INTO SUPPORT_TICKET (ID, CUSTOMER_ID, CUSTOMER_IDENTIFICATION, TITLE, DESCRIPTION, ASSIGNED_STAFF_ID, STATUS, CREATED_AT, UPDATED_AT) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, customer.getId());
            ps.setString(3, customer.getIdentificationNo());
            ps.setString(4, ticketTitle);
            ps.setString(5, description);
            ps.setString(6, null);
            ps.setString(7, TicketStatus.OPEN.name());
            ps.setTimestamp(8, Timestamp.valueOf(now));
            ps.setTimestamp(9, Timestamp.valueOf(now));
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Ticket creation failed: " + e.getMessage());
            return null;
        }

        System.out.println("Support ticket created with ID: " + id);
        return new SupportTicket(id, customerIdNumber, ticketTitle, description, null, TicketStatus.OPEN, now, now);
    }

    @Override
    public SupportTicket updateTicketDetails(String ticketId, String authPassword, String title, String description) {
        TicketRow row = loadTicket(ticketId);
        if (row == null) {
            System.out.println("Update failed: ticket not found for ID " + ticketId);
            return null;
        }
        String ownerLoginId = isBlank(row.customerIdentification) ? row.ticket.getAccountNumber() : row.customerIdentification;
        if (!customerService.verifyLogin(ownerLoginId, authPassword)) {
            System.out.println("Update failed: invalid customer credentials for ticket " + ticketId);
            return null;
        }
        if (row.ticket.getStatus() == TicketStatus.RESOLVED) {
            System.out.println("Update failed: ticket is already resolved " + ticketId);
            return null;
        }

        String newTitle = isBlank(title) ? row.ticket.getTitle() : title;
        String newDescription = isBlank(description) ? row.ticket.getDescription() : description;
        LocalDateTime now = LocalDateTime.now();

        String sql = "UPDATE SUPPORT_TICKET SET TITLE = ?, DESCRIPTION = ?, UPDATED_AT = ? WHERE ID = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newTitle);
            ps.setString(2, newDescription);
            ps.setTimestamp(3, Timestamp.valueOf(now));
            ps.setString(4, ticketId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Update failed: " + e.getMessage());
            return null;
        }

        return new SupportTicket(
            row.ticket.getId(),
            row.ticket.getAccountNumber(),
            newTitle,
            newDescription,
            row.ticket.getAssignedStaffId(),
            row.ticket.getStatus(),
            row.ticket.getCreatedAt(),
            now
        );
    }


    @Override
    public SupportTicket assignTicket(String ticketId, String staffId) {
        TicketRow row = loadTicket(ticketId);
        if (row == null) {
            System.out.println("Assignment failed: ticket not found for ID " + ticketId);
            return null;
        }
        if (isBlank(staffId)) {
            System.out.println("Assignment failed: staff ID is required.");
            return null;
        }
        if (row.ticket.getAssignedStaffId() != null) {
            System.out.println("Assignment failed: ticket already assigned to " + row.ticket.getAssignedStaffId());
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        String sql = "UPDATE SUPPORT_TICKET SET ASSIGNED_STAFF_ID = ?, UPDATED_AT = ? WHERE ID = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, staffId);
            ps.setTimestamp(2, Timestamp.valueOf(now));
            ps.setString(3, ticketId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Assignment failed: " + e.getMessage());
            return null;
        }

        return new SupportTicket(
            row.ticket.getId(),
            row.ticket.getAccountNumber(),
            row.ticket.getTitle(),
            row.ticket.getDescription(),
            staffId,
            row.ticket.getStatus(),
            row.ticket.getCreatedAt(),
            now
        );
    }

    @Override
    public SupportTicket updateTicketStatus(String ticketId, String staffId, TicketStatus newStatus) {
        TicketRow row = loadTicket(ticketId);
        if (row == null) {
            System.out.println("Status update failed: ticket not found for ID " + ticketId);
            return null;
        }
        if (newStatus == null) {
            System.out.println("Status update failed: new status is required.");
            return null;
        }

        String assigned = row.ticket.getAssignedStaffId();
        if (assigned == null) {
            assigned = staffId;
        } else if (!Objects.equals(assigned, staffId)) {
            System.out.println("Status update failed: ticket is assigned to " + assigned);
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        String sql = "UPDATE SUPPORT_TICKET SET ASSIGNED_STAFF_ID = ?, STATUS = ?, UPDATED_AT = ? WHERE ID = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, assigned);
            ps.setString(2, newStatus.name());
            ps.setTimestamp(3, Timestamp.valueOf(now));
            ps.setString(4, ticketId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Status update failed: " + e.getMessage());
            return null;
        }

        return new SupportTicket(
            row.ticket.getId(),
            row.ticket.getAccountNumber(),
            row.ticket.getTitle(),
            row.ticket.getDescription(),
            assigned,
            newStatus,
            row.ticket.getCreatedAt(),
            now
        );
    }

    @Override
    public List<SupportTicket> listTickets() {
        List<SupportTicket> result = new ArrayList<>();
        String sql = "SELECT ID, ACCOUNT_NUMBER, TITLE, DESCRIPTION, ASSIGNED_STAFF_ID, STATUS, CREATED_AT, UPDATED_AT FROM SUPPORT_TICKET";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapTicket(rs));
            }
        } catch (SQLException e) {
            System.out.println("Failed to list tickets: " + e.getMessage());
        }
        return result;
    }

    @Override
    public SupportTicket getTicket(String ticketId) {
        TicketRow row = loadTicket(ticketId);
        return row == null ? null : row.ticket;
    }

    private void initSchema(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS SUPPORT_TICKET ("
            + "ID VARCHAR(36) PRIMARY KEY, "
            + "CUSTOMER_ID VARCHAR(255) NOT NULL, "
            + "CUSTOMER_IDENTIFICATION VARCHAR(255), "
            + "TITLE VARCHAR(255) NOT NULL, "
            + "DESCRIPTION CLOB NOT NULL, "
            + "ASSIGNED_STAFF_ID VARCHAR(255), "
            + "STATUS VARCHAR(32) NOT NULL, "
            + "CREATED_AT TIMESTAMP NOT NULL, "
            + "UPDATED_AT TIMESTAMP NOT NULL"
            + ")";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    private TicketRow loadTicket(String ticketId) {
        String sql = "SELECT ID, CUSTOMER_ID, CUSTOMER_IDENTIFICATION, TITLE, DESCRIPTION, ASSIGNED_STAFF_ID, STATUS, CREATED_AT, UPDATED_AT "
            + "FROM SUPPORT_TICKET WHERE ID = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    SupportTicket ticket = mapTicket(rs);
                    String identification = rs.getString("CUSTOMER_IDENTIFICATION");
                    return new TicketRow(ticket, identification);
                }
            }
        } catch (SQLException e) {
            System.out.println("Lookup failed for ticket " + ticketId + ": " + e.getMessage());
        }
        return null;
    }

    private SupportTicket mapTicket(ResultSet rs) throws SQLException {
        return new SupportTicket(
            rs.getString("ID"),
            rs.getString("CUSTOMER_ID"),
            rs.getString("TITLE"),
            rs.getString("DESCRIPTION"),
            rs.getString("ASSIGNED_STAFF_ID"),
            TicketStatus.valueOf(rs.getString("STATUS")),
            toLocalDateTime(rs.getTimestamp("CREATED_AT")),
            toLocalDateTime(rs.getTimestamp("UPDATED_AT"))
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String coalesce(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private static final class TicketRow {
        private final SupportTicket ticket;
        private final String customerIdentification;

        private TicketRow(SupportTicket ticket, String customerIdentification) {
            this.ticket = ticket;
            this.customerIdentification = customerIdentification;
        }
    }
}
