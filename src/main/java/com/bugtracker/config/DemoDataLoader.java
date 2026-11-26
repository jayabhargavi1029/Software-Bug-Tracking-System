package com.bugtracker.config;

import com.bugtracker.dto.TicketForm;
import com.bugtracker.entity.BugTicket;
import com.bugtracker.entity.Category;
import com.bugtracker.entity.Priority;
import com.bugtracker.entity.Role;
import com.bugtracker.entity.Severity;
import com.bugtracker.entity.Status;
import com.bugtracker.entity.User;
import com.bugtracker.repository.BugTicketRepository;
import com.bugtracker.repository.UserRepository;
import com.bugtracker.service.TicketService;
import com.bugtracker.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Inserts demo users and tickets into the configured database on first start
 * (only when the users table is empty). The data goes through the normal services,
 * so ticket ids, history and passwords (BCrypt) are created exactly like real data.
 */
@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true", matchIfMissing = true)
public class DemoDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;
    private final TicketService ticketService;
    private final BugTicketRepository ticketRepository;

    public DemoDataLoader(UserRepository userRepository, UserService userService,
                          TicketService ticketService, BugTicketRepository ticketRepository) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.ticketService = ticketService;
        this.ticketRepository = ticketRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return;

        User admin = userService.createUser("admin", "Arjun Mehta", "admin@bugtrack.local", "Admin@123", Role.ADMIN);
        User priya = userService.createUser("priya", "Priya Sharma", "priya@bugtrack.local", "Dev@1234", Role.DEVELOPER);
        User rahul = userService.createUser("rahul", "Rahul Verma", "rahul@bugtrack.local", "Dev@1234", Role.DEVELOPER);
        User anita = userService.createUser("anita", "Anita Reddy", "anita@bugtrack.local", "Dev@1234", Role.DEVELOPER);

        seed(admin, admin, "Login page crashes on empty password",
                "Pressing the Login button with an empty password field shows a white error page instead of a validation message.",
                Category.UI, Priority.HIGH, Severity.MAJOR, priya, Status.IN_PROGRESS, 9,
                "I can reproduce this on Chrome and Edge. Checking the form validation now.", priya);
        seed(admin, priya, "Payment total is wrong when discount code is applied",
                "The cart total ignores the 10% discount when the code is applied twice. Expected 900, shown 1000.",
                Category.BACKEND, Priority.CRITICAL, Severity.BLOCKER, rahul, Status.OPEN, 8,
                null, null);
        seed(admin, rahul, "Slow response on the search page",
                "Searching with more than 3 words takes about 8 seconds on the staging server.",
                Category.PERFORMANCE, Priority.MEDIUM, Severity.MODERATE, anita, Status.IN_PROGRESS, 7,
                "An index on the title column should help. Testing it.", anita);
        seed(admin, admin, "Typo in the user manual, page 12",
                "The word 'authentification' should be 'authentication' in section 3.2.",
                Category.DOCUMENTATION, Priority.LOW, Severity.MINOR, priya, Status.RESOLVED, 7,
                "Fixed in the latest draft of the manual.", priya);
        seed(admin, priya, "SQL error when the name contains an apostrophe",
                "Registering a customer named O'Brien throws a database exception.",
                Category.DATABASE, Priority.HIGH, Severity.MAJOR, rahul, Status.CLOSED, 6,
                "Fixed with a parameterised query and verified by QA.", rahul);
        seed(admin, admin, "Session does not expire after logout",
                "After logging out the back button still shows the previous private page.",
                Category.SECURITY, Priority.CRITICAL, Severity.MAJOR, anita, Status.REOPENED, 5,
                "The issue came back after the last release.", admin);
        seed(admin, rahul, "Profile picture upload fails for PNG files",
                "Uploading a PNG larger than 2 MB returns HTTP 500.",
                Category.INTEGRATION, Priority.MEDIUM, Severity.MODERATE, null, Status.OPEN, 4,
                null, null);
        seed(admin, anita, "Dashboard cards overlap on small screens",
                "On a 360 px wide phone the four statistic cards overlap each other.",
                Category.UI, Priority.LOW, Severity.MINOR, priya, Status.OPEN, 3,
                null, null);
        seed(admin, admin, "Export to CSV downloads an empty file",
                "The export button creates a file with only the header row.",
                Category.BACKEND, Priority.HIGH, Severity.MODERATE, rahul, Status.IN_PROGRESS, 2,
                "Looks like the query ignores the selected date range.", rahul);
        seed(admin, priya, "Email notification sent twice",
                "Users receive two identical e-mails when a ticket is assigned to them.",
                Category.INTEGRATION, Priority.MEDIUM, Severity.MINOR, anita, Status.RESOLVED, 1,
                "Removed the duplicate listener.", anita);
        seed(admin, anita, "Date picker shows wrong month in Safari",
                "The calendar starts one month earlier than the selected date.",
                Category.UI, Priority.MEDIUM, Severity.MODERATE, null, Status.OPEN, 0,
                null, null);
        seed(admin, admin, "Backup job fails every Sunday night",
                "The weekly backup script stops with a 'disk quota exceeded' message.",
                Category.DATABASE, Priority.CRITICAL, Severity.BLOCKER, rahul, Status.OPEN, 0,
                null, null);
    }

    /** Creates one ticket, walks it through valid status changes, adds a comment and back-dates it. */
    private void seed(User admin, User reporter, String title, String description, Category category,
                      Priority priority, Severity severity, User assignee, Status finalStatus,
                      int daysAgo, String comment, User commentAuthor) {
        TicketForm form = new TicketForm();
        form.setTitle(title);
        form.setDescription(description);
        form.setCategory(category);
        form.setPriority(priority);
        form.setSeverity(severity);
        form.setAssignedToId(assignee == null ? null : assignee.getId());

        BugTicket ticket = ticketService.create(form, reporter);
        for (Status step : pathTo(finalStatus)) {
            ticketService.changeStatus(ticket.getId(), step, admin);
        }
        if (comment != null && commentAuthor != null) {
            ticketService.addComment(ticket.getId(), comment, commentAuthor);
        }
        BugTicket stored = ticketRepository.findById(ticket.getId()).orElseThrow();
        stored.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        ticketRepository.save(stored);
    }

    private List<Status> pathTo(Status target) {
        return switch (target) {
            case OPEN -> List.of();
            case IN_PROGRESS -> List.of(Status.IN_PROGRESS);
            case RESOLVED -> List.of(Status.IN_PROGRESS, Status.RESOLVED);
            case CLOSED -> List.of(Status.IN_PROGRESS, Status.RESOLVED, Status.CLOSED);
            case REOPENED -> List.of(Status.IN_PROGRESS, Status.RESOLVED, Status.REOPENED);
        };
    }
}
