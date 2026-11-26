package com.bugtracker.service;

import com.bugtracker.dto.TicketForm;
import com.bugtracker.entity.BugTicket;
import com.bugtracker.entity.Category;
import com.bugtracker.entity.Comment;
import com.bugtracker.entity.Priority;
import com.bugtracker.entity.Role;
import com.bugtracker.entity.Status;
import com.bugtracker.entity.TicketHistory;
import com.bugtracker.entity.User;
import com.bugtracker.exception.InvalidStatusChangeException;
import com.bugtracker.exception.ResourceNotFoundException;
import com.bugtracker.repository.BugTicketRepository;
import com.bugtracker.repository.CommentRepository;
import com.bugtracker.repository.TicketHistoryRepository;
import com.bugtracker.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.JoinType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** All business rules for tickets, comments and ticket history. */
@Service
public class TicketService {

    private final BugTicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final TicketHistoryRepository historyRepository;
    private final UserRepository userRepository;

    public TicketService(BugTicketRepository ticketRepository, CommentRepository commentRepository,
                         TicketHistoryRepository historyRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
    }

    // ------------------------------------------------------------------ create / update / delete

    @Transactional
    public BugTicket create(TicketForm form, User reporter) {
        BugTicket ticket = new BugTicket();
        ticket.setTitle(form.getTitle().trim());
        ticket.setDescription(form.getDescription().trim());
        ticket.setCategory(form.getCategory());
        ticket.setPriority(form.getPriority());
        ticket.setSeverity(form.getSeverity());
        ticket.setStatus(Status.OPEN);
        ticket.setReportedBy(reporter);
        ticket.setAssignedTo(resolveDeveloper(form.getAssignedToId()));

        BugTicket saved = ticketRepository.save(ticket);
        saved.setTicketId("BUG-" + (1000 + saved.getId()));   // unique ticket id, e.g. BUG-1001
        log(saved, reporter, "Ticket created", "Reported with priority " + saved.getPriority().getLabel());
        if (saved.getAssignedTo() != null) {
            log(saved, reporter, "Assignment changed", "Unassigned → " + saved.getAssignedTo().getFullName());
        }
        return saved;
    }

    @Transactional
    public BugTicket update(Long id, TicketForm form, User actor) {
        BugTicket ticket = getById(id);
        List<String> changed = new ArrayList<>();

        if (!ticket.getTitle().equals(form.getTitle().trim())) {
            ticket.setTitle(form.getTitle().trim());
            changed.add("title");
        }
        if (!ticket.getDescription().equals(form.getDescription().trim())) {
            ticket.setDescription(form.getDescription().trim());
            changed.add("description");
        }
        if (ticket.getCategory() != form.getCategory()) {
            ticket.setCategory(form.getCategory());
            changed.add("category");
        }
        if (ticket.getPriority() != form.getPriority()) {
            ticket.setPriority(form.getPriority());
            changed.add("priority");
        }
        if (ticket.getSeverity() != form.getSeverity()) {
            ticket.setSeverity(form.getSeverity());
            changed.add("severity");
        }
        if (!changed.isEmpty()) {
            log(ticket, actor, "Ticket edited", "Updated " + String.join(", ", changed));
        }
        applyAssignment(ticket, resolveDeveloper(form.getAssignedToId()), actor);
        return ticketRepository.save(ticket);
    }

    @Transactional
    public void delete(Long id) {
        BugTicket ticket = getById(id);
        commentRepository.deleteByTicket(ticket);
        historyRepository.deleteByTicket(ticket);
        ticketRepository.delete(ticket);
    }

    // ------------------------------------------------------------------ assignment and status

    @Transactional
    public BugTicket assign(Long id, Long developerId, User actor) {
        BugTicket ticket = getById(id);
        applyAssignment(ticket, resolveDeveloper(developerId), actor);
        return ticketRepository.save(ticket);
    }

    @Transactional
    public BugTicket changeStatus(Long id, Status newStatus, User actor) {
        BugTicket ticket = getById(id);
        if (!canUpdateStatus(ticket, actor)) {
            throw new AccessDeniedException("Only the administrator or the assigned developer can change the status.");
        }
        Status old = ticket.getStatus();
        if (!old.canMoveTo(newStatus)) {
            throw new InvalidStatusChangeException(
                    "A ticket cannot move from " + old.getLabel() + " to " + newStatus.getLabel() + ".");
        }
        ticket.setStatus(newStatus);
        log(ticket, actor, "Status changed", old.getLabel() + " → " + newStatus.getLabel());
        return ticketRepository.save(ticket);
    }

    @Transactional
    public Comment addComment(Long id, String message, User author) {
        BugTicket ticket = getById(id);
        if (!canView(ticket, author)) {
            throw new AccessDeniedException("You are not allowed to comment on this ticket.");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Comment cannot be empty.");
        }
        if (message.trim().length() > 2000) {
            throw new IllegalArgumentException("Comment must be at most 2000 characters.");
        }
        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setAuthor(author);
        comment.setMessage(message.trim());
        return commentRepository.save(comment);
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public BugTicket getById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
    }

    @Transactional(readOnly = true)
    public BugTicket getForViewer(Long id, User viewer) {
        BugTicket ticket = getById(id);
        if (!canView(ticket, viewer)) {
            throw new AccessDeniedException("You are not allowed to view this ticket.");
        }
        return ticket;
    }

    /** Search by ticket id or title and filter by status, priority, category and assigned developer. */
    @Transactional(readOnly = true)
    public List<BugTicket> search(String query, Status status, Priority priority, Category category, Long developerId) {
        return search(query, status, priority, category, developerId, null);
    }

    /** Search tickets visible to a developer: those they reported or are assigned to. */
    @Transactional(readOnly = true)
    public List<BugTicket> searchVisibleTo(String query, Status status, Priority priority, Category category,
                                           User viewer) {
        return search(query, status, priority, category, null, viewer);
    }

    private List<BugTicket> search(String query, Status status, Priority priority, Category category,
                                   Long developerId, User viewer) {
        Specification<BugTicket> spec = (root, cq, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase() + "%";
                conditions.add(cb.or(
                        cb.like(cb.lower(root.get("ticketId")), like),
                        cb.like(cb.lower(root.get("title")), like)));
            }
            if (status != null) conditions.add(cb.equal(root.get("status"), status));
            if (priority != null) conditions.add(cb.equal(root.get("priority"), priority));
            if (category != null) conditions.add(cb.equal(root.get("category"), category));
            if (developerId != null) conditions.add(cb.equal(root.get("assignedTo").get("id"), developerId));
            if (viewer != null && viewer.getRole() == Role.DEVELOPER) {
                conditions.add(cb.or(
                        cb.equal(root.join("assignedTo", JoinType.LEFT).get("id"), viewer.getId()),
                        cb.equal(root.get("reportedBy").get("id"), viewer.getId())));
            }
            return cb.and(conditions.toArray(new Predicate[0]));
        };
        return ticketRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "id"));
    }

    @Transactional(readOnly = true)
    public List<Comment> commentsFor(BugTicket ticket) {
        return commentRepository.findByTicketOrderByCreatedAtAscIdAsc(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketHistory> historyFor(BugTicket ticket) {
        return historyRepository.findByTicketOrderByIdDesc(ticket);
    }

    // ------------------------------------------------------------------ permissions

    /** Admins see every ticket; developers see tickets assigned to them or reported by them. */
    public boolean canView(BugTicket ticket, User user) {
        return user.getRole() == Role.ADMIN || isSame(ticket.getAssignedTo(), user) || isSame(ticket.getReportedBy(), user);
    }

    /** Admins and the assigned developer may change the status. */
    public boolean canUpdateStatus(BugTicket ticket, User user) {
        return user.getRole() == Role.ADMIN || isSame(ticket.getAssignedTo(), user);
    }

    // ------------------------------------------------------------------ helpers

    private boolean isSame(User a, User b) {
        return a != null && b != null && a.getId().equals(b.getId());
    }

    private User resolveDeveloper(Long developerId) {
        if (developerId == null) return null;
        User user = userRepository.findById(developerId)
                .orElseThrow(() -> new ResourceNotFoundException("Developer not found"));
        if (user.getRole() != Role.DEVELOPER || !user.isActive()) {
            throw new IllegalArgumentException("The selected user is not an active developer.");
        }
        return user;
    }

    private void applyAssignment(BugTicket ticket, User developer, User actor) {
        Long oldId = ticket.getAssignedTo() == null ? null : ticket.getAssignedTo().getId();
        Long newId = developer == null ? null : developer.getId();
        if (Objects.equals(oldId, newId)) return;
        String from = ticket.getAssignedTo() == null ? "Unassigned" : ticket.getAssignedTo().getFullName();
        String to = developer == null ? "Unassigned" : developer.getFullName();
        ticket.setAssignedTo(developer);
        log(ticket, actor, "Assignment changed", from + " → " + to);
    }

    private void log(BugTicket ticket, User actor, String action, String details) {
        TicketHistory entry = new TicketHistory();
        entry.setTicket(ticket);
        entry.setChangedBy(actor);
        entry.setAction(action);
        entry.setDetails(details);
        historyRepository.save(entry);
    }
}
