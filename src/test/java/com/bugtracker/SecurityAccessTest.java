package com.bugtracker;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.bugtracker.dto.TicketForm;
import com.bugtracker.entity.BugTicket;
import com.bugtracker.entity.Category;
import com.bugtracker.entity.Priority;
import com.bugtracker.entity.Role;
import com.bugtracker.entity.Severity;
import com.bugtracker.entity.User;
import com.bugtracker.service.TicketService;
import com.bugtracker.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Tests that pages and actions are protected by role (Spring Security + MVC). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityAccessTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserService userService;
    @Autowired private TicketService ticketService;

    private User admin;
    private User dev;
    private BugTicket ticket;

    @BeforeEach
    void setUp() {
        admin = userService.createUser("sec_admin", "Sec Admin", "sec_admin@test.local", "secret123", Role.ADMIN);
        dev = userService.createUser("sec_dev", "Sec Dev", "sec_dev@test.local", "secret123", Role.DEVELOPER);
        userService.createUser("sec_other", "Sec Other", "sec_other@test.local", "secret123", Role.DEVELOPER);

        TicketForm form = new TicketForm();
        form.setTitle("Security test ticket");
        form.setDescription("Ticket used by the role-based access tests.");
        form.setCategory(Category.SECURITY);
        form.setPriority(Priority.HIGH);
        form.setSeverity(Severity.MAJOR);
        form.setAssignedToId(dev.getId());
        ticket = ticketService.create(form, admin);
    }

    @Test
    void anonymousUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/tickets"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void landingPageIsPublic() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("landing"));
    }

    @Test
    void adminCanOpenAdminDashboardUsersAndTickets() throws Exception {
        mockMvc.perform(get("/admin/dashboard").with(user("sec_admin").roles("ADMIN"))).andExpect(status().isOk());
        mockMvc.perform(get("/admin/users").with(user("sec_admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Remove login access"))));
        mockMvc.perform(get("/admin/users/" + dev.getId()).with(user("sec_admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("user-details"))
                .andExpect(content().string(containsString("sec_dev@test.local")))
                .andExpect(content().string(containsString("Remove login access")));
        mockMvc.perform(get("/tickets").with(user("sec_admin").roles("ADMIN"))).andExpect(status().isOk());
        mockMvc.perform(get("/tickets/" + ticket.getId()).with(user("sec_admin").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    void onlyAdminsCanRemoveUserAccess() throws Exception {
        mockMvc.perform(post("/admin/users/" + dev.getId() + "/remove-access")
                        .with(user("sec_dev").roles("DEVELOPER")).with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/users/" + dev.getId() + "/remove-access")
                        .with(user("sec_admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/" + dev.getId()));

        assertTrue(userService.findAll().stream().anyMatch(user -> user.getId().equals(dev.getId())
                && user.isAccessRevoked() && user.getFullName().equals("Sec Dev")));
        assertEquals(dev.getId(), ticketService.getById(ticket.getId()).getAssignedTo().getId());
    }

    @Test
    void developerCannotOpenAdminPages() throws Exception {
        mockMvc.perform(get("/admin/dashboard").with(user("sec_dev").roles("DEVELOPER"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/users").with(user("sec_dev").roles("DEVELOPER"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/users/" + dev.getId()).with(user("sec_dev").roles("DEVELOPER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotOpenDeveloperDashboard() throws Exception {
        mockMvc.perform(get("/developer/dashboard").with(user("sec_admin").roles("ADMIN"))).andExpect(status().isForbidden());
    }

    @Test
    void developerCanOpenOwnDashboardAndAssignedTicket() throws Exception {
        mockMvc.perform(get("/developer/dashboard").with(user("sec_dev").roles("DEVELOPER"))).andExpect(status().isOk());
        mockMvc.perform(get("/tickets/" + ticket.getId()).with(user("sec_dev").roles("DEVELOPER"))).andExpect(status().isOk());
    }

    @Test
    void developerCannotViewSomeoneElsesTicket() throws Exception {
        mockMvc.perform(get("/tickets/" + ticket.getId()).with(user("sec_other").roles("DEVELOPER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void developerCannotDeleteOrEditTickets() throws Exception {
        mockMvc.perform(post("/tickets/" + ticket.getId() + "/delete").with(user("sec_dev").roles("DEVELOPER")).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/tickets/" + ticket.getId() + "/edit").with(user("sec_dev").roles("DEVELOPER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanDeleteATicket() throws Exception {
        mockMvc.perform(post("/tickets/" + ticket.getId() + "/delete").with(user("sec_admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"));
    }

    @Test
    void postWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/tickets/" + ticket.getId() + "/delete").with(user("sec_admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }
}
