package com.bank.api;

import java.util.List;

/**
 * API for customer support ticket operations.
 */
public interface ISupportTicketService {
    SupportTicket createTicket(String accountNumber, String profilePassword, String title, String description);
    SupportTicket updateTicketDetails(String ticketId, String profilePassword, String title, String description);
    SupportTicket assignTicket(String ticketId, String staffId);
    SupportTicket updateTicketStatus(String ticketId, String staffId, TicketStatus newStatus);
    List<SupportTicket> listTickets();
    SupportTicket getTicket(String ticketId);
}
