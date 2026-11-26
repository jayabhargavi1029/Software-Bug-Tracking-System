package com.bugtracker.dto;

import com.bugtracker.entity.BugTicket;
import com.bugtracker.entity.Category;
import com.bugtracker.entity.Priority;
import com.bugtracker.entity.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Data typed in the "Report Bug" / "Edit Ticket" form, with validation rules. */
public class TicketForm {

    @NotBlank(message = "Bug title is required")
    @Size(max = 150, message = "Title must be at most 150 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 4000, message = "Description must be between 10 and 4000 characters")
    private String description;

    @NotNull(message = "Please choose a category")
    private Category category;

    @NotNull(message = "Please choose a priority")
    private Priority priority;

    @NotNull(message = "Please choose a severity")
    private Severity severity;

    /** Optional - id of the developer the ticket is assigned to. */
    private Long assignedToId;

    public static TicketForm from(BugTicket ticket) {
        TicketForm form = new TicketForm();
        form.setTitle(ticket.getTitle());
        form.setDescription(ticket.getDescription());
        form.setCategory(ticket.getCategory());
        form.setPriority(ticket.getPriority());
        form.setSeverity(ticket.getSeverity());
        form.setAssignedToId(ticket.getAssignedTo() == null ? null : ticket.getAssignedTo().getId());
        return form;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public Long getAssignedToId() { return assignedToId; }
    public void setAssignedToId(Long assignedToId) { this.assignedToId = assignedToId; }
}
