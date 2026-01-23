package com.bank.support;

import static org.junit.jupiter.api.Assertions.*;

import com.bank.api.Customer;
import com.bank.api.SupportTicket;
import com.bank.api.TicketStatus;
import com.bank.api.ICustomerService;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SupportTicketServiceImplTest {

    private SupportTicketServiceImpl service;
    private FakeCustomerService customerService;
    private DataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:support-test;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        dataSource = ds;

        customerService = new FakeCustomerService();

        service = new SupportTicketServiceImpl();
        inject(service, "customerService", customerService);
        inject(service, "dataSource", dataSource);
        service.activate(); // create schema
        clearTable();
    }

    @AfterEach
    void tearDown() throws SQLException {
        clearTable();
    }

    @Test
    void createTicket_happyPath_persistsOpenTicket() {
        SupportTicket created = createTicketFor("ID001", "pass", "Card issue", "Help me");

        assertNotNull(created);
        assertEquals(TicketStatus.OPEN, created.getStatus());
        assertNull(created.getAssignedStaffId());

        SupportTicket fetched = service.getTicket(created.getId());
        assertNotNull(fetched, "Ticket should be persisted and retrievable");
        assertEquals("Card issue", fetched.getTitle());
        assertEquals(TicketStatus.OPEN, fetched.getStatus());
    }

    @Test
    void createTicket_invalidCredentials_returnsNull() {
        SupportTicket created = service.createTicket("ID001", "bad", "Title", "Desc");

        assertNull(created);
    }

    @Test
    void createTicket_missingRequiredFields_returnsNull() {
        SupportTicket noDesc = service.createTicket("ID001", "pass", "Title", " ");
        assertNull(noDesc);

        SupportTicket noId = service.createTicket(" ", "pass", "Title", "Desc");
        assertNull(noId);
    }

    @Test
    void updateTicketDetails_onlyOwnerCanEdit() {
        SupportTicket created = createTicketFor("ID001", "good", "Old", "Body");

        SupportTicket rejected = service.updateTicketDetails(created.getId(), "bad", "New", "NewDesc");
        assertNull(rejected, "Non-owner should not be able to edit");

        SupportTicket updated = service.updateTicketDetails(created.getId(), "good", "New", "NewDesc");
        assertNotNull(updated);
        assertEquals("New", updated.getTitle());
        assertEquals("NewDesc", updated.getDescription());
    }

    @Test
    void updateTicketDetails_resolvedTicketRejected() {
        SupportTicket created = createTicketFor("ID001", "good", "Old", "Body");

        SupportTicket resolved = service.updateTicketStatus(created.getId(), "staff1", TicketStatus.RESOLVED);
        assertNotNull(resolved);

        SupportTicket rejected = service.updateTicketDetails(created.getId(), "good", "New", "NewDesc");
        assertNull(rejected, "Resolved tickets should not be editable");
    }

    @Test
    void assignTicket_onlyOnce_andRequiresStaffId() {
        SupportTicket created = createTicketFor("ID001", "good", "Need help", "Body");

        SupportTicket missingStaff = service.assignTicket(created.getId(), " ");
        assertNull(missingStaff);

        SupportTicket assigned = service.assignTicket(created.getId(), "agent-1");
        assertNotNull(assigned);
        assertEquals("agent-1", assigned.getAssignedStaffId());

        SupportTicket secondAttempt = service.assignTicket(created.getId(), "agent-2");
        assertNull(secondAttempt, "Ticket should not be reassigned once set");
    }

    @Test
    void updateTicketStatus_setsAssigneeOnFirstChange() {
        SupportTicket created = createTicketFor("ID001", "good", "Need help", "Body");

        SupportTicket inProgress = service.updateTicketStatus(created.getId(), "agent-1", TicketStatus.IN_PROGRESS);

        assertNotNull(inProgress);
        assertEquals("agent-1", inProgress.getAssignedStaffId());
        assertEquals(TicketStatus.IN_PROGRESS, inProgress.getStatus());
    }

    @Test
    void updateTicketStatus_wrongStaffRejected() {
        SupportTicket created = createTicketFor("ID001", "good", "Need help", "Body");
        SupportTicket assigned = service.assignTicket(created.getId(), "agent-1");
        assertNotNull(assigned);

        SupportTicket rejected = service.updateTicketStatus(created.getId(), "agent-2", TicketStatus.IN_PROGRESS);
        assertNull(rejected, "Different staff cannot update status");
    }

    @Test
    void updateTicketStatus_nullStatusRejected() {
        SupportTicket created = createTicketFor("ID001", "good", "Need help", "Body");

        SupportTicket rejected = service.updateTicketStatus(created.getId(), "agent-1", null);

        assertNull(rejected);
    }

    private SupportTicket createTicketFor(String customerId, String password, String title, String description) {
        Customer customer = new Customer();
        customer.setId("cust-" + customerId);
        customer.setIdentificationNo(customerId);
        customerService.registerCustomer(customer, password);

        return service.createTicket(customerId, password, title, description);
    }

    private void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void clearTable() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().executeUpdate("DELETE FROM SUPPORT_TICKET");
        }
    }

    private static final class FakeCustomerService implements ICustomerService {
        private final Map<String, CustomerRecord> records = new HashMap<>();

        void registerCustomer(Customer customer, String password) {
            if (customer == null) return;
            CustomerRecord record = new CustomerRecord(customer, password);
            put(customer.getId(), record);
            put(customer.getIdentificationNo(), record);
        }

        @Override
        public Customer createCustomer(String name, String email) {
            throw new UnsupportedOperationException("Not needed for tests");
        }

        @Override
        public Customer updateCustomer(String id, String name, String email) {
            throw new UnsupportedOperationException("Not needed for tests");
        }

        @Override
        public Customer getCustomer(String id) {
            CustomerRecord record = records.get(id);
            return record == null ? null : record.customer;
        }

        @Override
        public boolean verifyLogin(String id, String password) {
            CustomerRecord record = records.get(id);
            return record != null && record.password.equals(password);
        }

        private void put(String key, CustomerRecord record) {
            if (key != null) {
                records.put(key, record);
            }
        }

        private static final class CustomerRecord {
            private final Customer customer;
            private final String password;

            private CustomerRecord(Customer customer, String password) {
                this.customer = customer;
                this.password = password;
            }
        }
    }
}
