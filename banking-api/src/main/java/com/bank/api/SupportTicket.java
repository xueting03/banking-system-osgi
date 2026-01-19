package com.bank.api;

import java.time.LocalDateTime;

/**
 * Immutable snapshot of a support ticket.
 */
public class SupportTicket {
    private final String id;
    private final String accountNumber;
    private final String title;
    private final String description;
    private final String assignedStaffId;
    private final TicketStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public SupportTicket(
        String id,
        String accountNumber,
        String title,
        String description,
        String assignedStaffId,
        TicketStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.title = title;
        this.description = description;
        this.assignedStaffId = assignedStaffId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getAssignedStaffId() {
        return assignedStaffId;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
