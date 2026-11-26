package com.bugtracker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One line of the audit trail of a ticket (created, status changed, assigned, edited). */
@Entity
@Table(name = "ticket_history")
public class TicketHistory extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "ticket_id", nullable = false)
    private BugTicket ticket;

    @ManyToOne
    @JoinColumn(name = "changed_by_id", nullable = false)
    private User changedBy;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(length = 500)
    private String details;

    public BugTicket getTicket() { return ticket; }
    public void setTicket(BugTicket ticket) { this.ticket = ticket; }
    public User getChangedBy() { return changedBy; }
    public void setChangedBy(User changedBy) { this.changedBy = changedBy; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
