package com.bugtracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bugtracker.dto.DashboardStats;
import com.bugtracker.dto.TicketForm;
import com.bugtracker.entity.BugTicket;
import com.bugtracker.entity.Category;
import com.bugtracker.entity.Priority;
import com.bugtracker.entity.Role;
import com.bugtracker.entity.Severity;
import com.bugtracker.entity.Status;
import com.bugtracker.entity.User;
import com.bugtracker.exception.DuplicateUserException;
import com.bugtracker.exception.InvalidStatusChangeException;
import com.bugtracker.exception.ResourceNotFoundException;
import com.bugtracker.repository.BugTicketRepository;
import com.bugtracker.repository.CommentRepository;
import com.bugtracker.repository.TicketHistoryRepository;
import com.bugtracker.security.CustomUserDetailsService;
import com.bugtracker.service.DashboardService;
import com.bugtracker.service.TicketService;
import com.bugtracker.service.UserService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the service layer. They run against an in-memory H2 database
 * (profile "test"), and every test is rolled back, so tests do not affect each other.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TicketServiceTest {

    @Autowired private TicketService ticketService;
    @Autowired private UserService userService;
    @Autowired private DashboardService dashboardService;
    @Autowired private BugTicketRepository ticketRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private TicketHistoryRepository historyRepository;
    @Autowired private CustomUserDetailsService userDetailsService;

    private User admin;
    private User dev;
    private User otherDev;

    @BeforeEach
    void setUp() {
        admin = userService.createUser("admin_t", "Test Admin", "admin_t@test.local", "secret123", Role.ADMIN);
        dev = userService.createUser("dev_t", "Test Developer", "dev_t@test.local", "secret123", Role.DEVELOPER);
        otherDev = userService.createUser("dev_o", "Other Developer", "dev_o@test.local", "secret123", Role.DEVELOPER);
    }

    private TicketForm form(String title, Priority priority, Category category, Long developerId) {
        TicketForm form = new TicketForm();
        form.setTitle(title);
        form.setDescription("Steps to reproduce: open the page and click the button.");
        form.setCategory(category);
        form.setPriority(priority);
        form.setSeverity(Severity.MAJOR);
        form.setAssignedToId(developerId);
        return form;
    }

    // ---------------------------------------------------------------- creating a bug

    @Test
    void creatingABugGeneratesTicketIdOpenStatusAndHistory() {
        BugTicket ticket = ticketService.create(form("Login fails", Priority.HIGH, Category.SECURITY, dev.getId()), admin);

        assertNotNull(ticket.getId());
        assertTrue(ticket.getTicketId().startsWith("BUG-"));
        assertEquals(Status.OPEN, ticket.getStatus());
        assertEquals(dev.getId(), ticket.getAssignedTo().getId());
        assertEquals(admin.getId(), ticket.getReportedBy().getId());
        assertNotNull(ticket.getCreatedAt());
        assertFalse(ticketService.historyFor(ticket).isEmpty());
    }

    @Test
    void everyTicketGetsADifferentTicketId() {
        BugTicket first = ticketService.create(form("First", Priority.LOW, Category.UI, null), admin);
        BugTicket second = ticketService.create(form("Second", Priority.LOW, Category.UI, null), admin);
        assertFalse(first.getTicketId().equals(second.getTicketId()));
    }

    @Test
    void assigningToANonDeveloperIsRejected() {
        TicketForm bad = form("Bad assignment", Priority.LOW, Category.UI, admin.getId());
        assertThrows(IllegalArgumentException.class, () -> ticketService.create(bad, admin));
    }

    // ---------------------------------------------------------------- retrieving tickets

    @Test
    void ticketsCanBeSearchedByTitleIdAndFilteredByPriorityStatusAndCategory() {
        BugTicket a = ticketService.create(form("Payment page crash", Priority.CRITICAL, Category.BACKEND, dev.getId()), admin);
        ticketService.create(form("Typo in footer", Priority.LOW, Category.UI, null), admin);
        ticketService.create(form("Slow report export", Priority.MEDIUM, Category.PERFORMANCE, otherDev.getId()), admin);

        assertEquals(3, ticketService.search(null, null, null, null, null).size());
        assertEquals(1, ticketService.search("payment", null, null, null, null).size());          // by title
        assertEquals(1, ticketService.search(a.getTicketId(), null, null, null, null).size());    // by ticket id
        assertEquals(1, ticketService.search(null, null, Priority.LOW, null, null).size());       // by priority
        assertEquals(1, ticketService.search(null, null, null, Category.PERFORMANCE, null).size());
        assertEquals(1, ticketService.search(null, null, null, null, dev.getId()).size());        // by developer
        assertEquals(3, ticketService.search(null, Status.OPEN, null, null, null).size());
        assertEquals(0, ticketService.search(null, Status.CLOSED, null, null, null).size());
    }

    @Test
    void developerSearchIncludesReportedAndAssignedTicketsOnly() {
        BugTicket reported = ticketService.create(form("Reported by developer", Priority.HIGH, Category.UI, null), dev);
        BugTicket assigned = ticketService.create(form("Assigned to developer", Priority.MEDIUM, Category.BACKEND,
                dev.getId()), admin);
        BugTicket unrelated = ticketService.create(form("Other developer ticket", Priority.LOW, Category.DATABASE,
                otherDev.getId()), admin);

        List<BugTicket> visible = ticketService.searchVisibleTo(null, null, null, null, dev);

        assertEquals(2, visible.size());
        assertTrue(visible.stream().anyMatch(ticket -> ticket.getId().equals(reported.getId())));
        assertTrue(visible.stream().anyMatch(ticket -> ticket.getId().equals(assigned.getId())));
        assertFalse(visible.stream().anyMatch(ticket -> ticket.getId().equals(unrelated.getId())));
    }

    @Test
    void gettingAMissingTicketThrowsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> ticketService.getById(999_999L));
    }

    // ---------------------------------------------------------------- updating status

    @Test
    void statusCanBeUpdatedByAssignedDeveloperAndIsRecordedInHistory() {
        BugTicket ticket = ticketService.create(form("Crash", Priority.HIGH, Category.BACKEND, dev.getId()), admin);

        ticketService.changeStatus(ticket.getId(), Status.IN_PROGRESS, dev);
        ticketService.changeStatus(ticket.getId(), Status.RESOLVED, dev);

        assertEquals(Status.RESOLVED, ticketService.getById(ticket.getId()).getStatus());
        // created + 2 status changes (+ assignment line written at creation)
        assertTrue(historyRepository.countByTicket(ticket) >= 4);
    }

    @Test
    void invalidStatusChangeIsRejected() {
        BugTicket ticket = ticketService.create(form("Crash", Priority.HIGH, Category.BACKEND, dev.getId()), admin);
        assertThrows(InvalidStatusChangeException.class,
                () -> ticketService.changeStatus(ticket.getId(), Status.REOPENED, admin));   // OPEN -> REOPENED not allowed
    }

    @Test
    void developerNotAssignedCannotChangeStatus() {
        BugTicket ticket = ticketService.create(form("Crash", Priority.HIGH, Category.BACKEND, dev.getId()), admin);
        assertThrows(AccessDeniedException.class,
                () -> ticketService.changeStatus(ticket.getId(), Status.IN_PROGRESS, otherDev));
    }

    @Test
    void commentsCanBeAddedAndReadBack() {
        BugTicket ticket = ticketService.create(form("Crash", Priority.HIGH, Category.BACKEND, dev.getId()), admin);
        ticketService.addComment(ticket.getId(), "Looking into it.", dev);

        assertEquals(1, ticketService.commentsFor(ticket).size());
        assertEquals("Looking into it.", ticketService.commentsFor(ticket).get(0).getMessage());
        assertThrows(IllegalArgumentException.class, () -> ticketService.addComment(ticket.getId(), "   ", dev));
    }

    // ---------------------------------------------------------------- deleting tickets

    @Test
    void deletingATicketAlsoRemovesItsCommentsAndHistory() {
        BugTicket ticket = ticketService.create(form("To delete", Priority.LOW, Category.UI, dev.getId()), admin);
        ticketService.addComment(ticket.getId(), "A comment", admin);
        Long id = ticket.getId();

        ticketService.delete(id);

        assertFalse(ticketRepository.findById(id).isPresent());
        assertEquals(0, commentRepository.count());
        assertEquals(0, historyRepository.count());
    }

    // ---------------------------------------------------------------- dashboard statistics

    @Test
    void dashboardStatisticsComeFromTheDatabase() {
        BugTicket t1 = ticketService.create(form("One", Priority.CRITICAL, Category.BACKEND, dev.getId()), admin);
        BugTicket t2 = ticketService.create(form("Two", Priority.HIGH, Category.UI, dev.getId()), admin);
        BugTicket t3 = ticketService.create(form("Three", Priority.LOW, Category.UI, otherDev.getId()), admin);
        ticketService.create(form("Four", Priority.MEDIUM, Category.UI, null), admin);

        ticketService.changeStatus(t1.getId(), Status.IN_PROGRESS, admin);
        ticketService.changeStatus(t2.getId(), Status.RESOLVED, admin);
        ticketService.changeStatus(t3.getId(), Status.CLOSED, admin);

        DashboardStats stats = dashboardService.adminStats();
        assertEquals(4, stats.getTotal());
        assertEquals(1, stats.getOpen());
        assertEquals(1, stats.getInProgress());
        assertEquals(1, stats.getResolved());
        assertEquals(1, stats.getClosed());
        assertEquals(1, stats.getCritical());
        assertEquals(5, stats.getByStatus().size());
        assertEquals(4, stats.getByPriority().size());
        assertEquals(4, stats.getRecent().size());

        DashboardStats devStats = dashboardService.developerStats(dev);
        assertEquals(2, devStats.getTotal());
        assertEquals(1, devStats.getInProgress());
        assertEquals(1, devStats.getResolved());
    }

    // ---------------------------------------------------------------- users

    @Test
    void duplicateUsernameAndEmailAreRejected() {
        assertThrows(DuplicateUserException.class,
                () -> userService.createUser("DEV_T", "Copy", "copy@test.local", "secret123", Role.DEVELOPER));
        assertThrows(DuplicateUserException.class,
                () -> userService.createUser("someone", "Copy", "dev_t@test.local", "secret123", Role.DEVELOPER));
    }

    @Test
    void passwordsAreStoredAsBcryptHashes() {
        List<User> users = userService.findAll();
        assertTrue(users.stream().noneMatch(u -> u.getPassword().equals("secret123")));
        assertTrue(users.stream().allMatch(u -> u.getPassword().startsWith("$2")));
    }

    @Test
    void removingUserAccessRetainsIdentityAndTicketRecords() {
        BugTicket ticket = ticketService.create(form("Keep history", Priority.HIGH, Category.SECURITY, dev.getId()), admin);
        ticketService.changeStatus(ticket.getId(), Status.IN_PROGRESS, dev);
        ticketService.addComment(ticket.getId(), "Investigation started.", dev);
        String formerName = dev.getFullName();
        String formerUsername = dev.getUsername();
        String formerEmail = dev.getEmail();

        User removed = userService.removeAccess(dev.getId(), admin);

        assertTrue(removed.isAccessRevoked());
        assertFalse(removed.isActive());
        assertFalse(removed.isAnonymized());
        assertEquals(formerName, removed.getFullName());
        assertEquals(formerUsername, removed.getUsername());
        assertEquals(formerEmail, removed.getEmail());
        assertFalse(userDetailsService.loadUserByUsername(formerUsername).isEnabled());

        BugTicket preserved = ticketService.getById(ticket.getId());
        assertEquals(removed.getId(), preserved.getAssignedTo().getId());
        assertEquals(formerName, preserved.getAssignedTo().getFullName());
        assertEquals(removed.getId(), ticketService.commentsFor(preserved).get(0).getAuthor().getId());
        assertEquals(formerName, ticketService.commentsFor(preserved).get(0).getAuthor().getFullName());
        assertTrue(ticketService.historyFor(preserved).stream()
                .anyMatch(entry -> entry.getChangedBy().getId().equals(removed.getId())
                        && entry.getChangedBy().getFullName().equals(formerName)));
        assertThrows(IllegalArgumentException.class, () -> userService.toggleActive(removed.getId(), admin));
        assertThrows(IllegalArgumentException.class, () -> userService.removeAccess(removed.getId(), admin));
    }

    @Test
    void removingAccessFromSelfOrLastActiveAdministratorIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> userService.removeAccess(admin.getId(), admin));
        assertThrows(IllegalArgumentException.class, () -> userService.removeAccess(admin.getId(), dev));
        assertTrue(userService.getByUsername(admin.getUsername()).isActive());
    }
}
