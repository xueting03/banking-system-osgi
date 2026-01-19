package com.bank.cli;

import com.bank.api.ISupportTicketService;
import com.bank.api.SupportTicket;
import com.bank.api.TicketStatus;
import java.util.List;

/**
 * Gogo commands for support ticket operations.
 * Scope: support
 */
public class SupportCommands {

    private final ISupportTicketService supportService;

    public SupportCommands(ISupportTicketService supportService) {
        this.supportService = supportService;
    }

    /**
     * support:create <accountNumber> <password> <title> <description>
     */
    public String create(String accountNumber, String password, String title, String description) {
        SupportTicket ticket = supportService.createTicket(accountNumber, password, title, description);
        return ticket == null ? "Failed to create ticket" : format(ticket);
    }

    /**
     * support:update <ticketId> <password> <title> <description>
     */
    public String update(String ticketId, String password, String title, String description) {
        SupportTicket ticket = supportService.updateTicketDetails(ticketId, password, title, description);
        return ticket == null ? "Failed to update ticket" : format(ticket);
    }

    /**
     * support:assign <ticketId> <staffId>
     */
    public String assign(String ticketId, String staffId) {
        SupportTicket ticket = supportService.assignTicket(ticketId, staffId);
        return ticket == null ? "Failed to assign ticket" : format(ticket);
    }

    /**
     * support:status <ticketId> <staffId> <OPEN|IN_PROGRESS|RESOLVED>
     */
    public String status(String ticketId, String staffId, String status) {
        TicketStatus parsed;
        try {
            parsed = TicketStatus.valueOf(status.toUpperCase());
        } catch (Exception e) {
            return "Invalid status. Use OPEN, IN_PROGRESS, or RESOLVED.";
        }
        SupportTicket ticket = supportService.updateTicketStatus(ticketId, staffId, parsed);
        return ticket == null ? "Failed to update status" : format(ticket);
    }

    /**
     * support:list
     */
    public String list() {
        List<SupportTicket> tickets = supportService.listTickets();
        if (tickets == null || tickets.isEmpty()) {
            return "No tickets found.";
        }
        StringBuilder sb = new StringBuilder();
        for (SupportTicket t : tickets) {
            sb.append(format(t)).append(System.lineSeparator());
        }
        return sb.toString().trim();
    }

    /**
     * support:get <ticketId>
     */
    public String get(String ticketId) {
        SupportTicket ticket = supportService.getTicket(ticketId);
        return ticket == null ? "Ticket not found" : format(ticket);
    }

    private String format(SupportTicket ticket) {
        return String.format(
            "Ticket %s | acct=%s | title=%s | assigned=%s | status=%s | created=%s | updated=%s",
            ticket.getId(),
            ticket.getAccountNumber(),
            ticket.getTitle(),
            ticket.getAssignedStaffId(),
            ticket.getStatus(),
            ticket.getCreatedAt(),
            ticket.getUpdatedAt()
        );
    }
}
