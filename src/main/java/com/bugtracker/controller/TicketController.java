package com.bugtracker.controller;

import com.bugtracker.dto.TicketForm;
import com.bugtracker.entity.BugTicket;
import com.bugtracker.entity.Category;
import com.bugtracker.entity.Priority;
import com.bugtracker.entity.Role;
import com.bugtracker.entity.Severity;
import com.bugtracker.entity.Status;
import com.bugtracker.entity.User;
import com.bugtracker.service.TicketService;
import com.bugtracker.service.UserService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** All ticket pages: list, report, view, edit, delete, assign, status and comments. */
@Controller
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final UserService userService;

    public TicketController(TicketService ticketService, UserService userService) {
        this.ticketService = ticketService;
        this.userService = userService;
    }

    // ---------------------------------------------------------------- list with search and filters

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) Status status,
                       @RequestParam(required = false) Priority priority,
                       @RequestParam(required = false) Category category,
                       @RequestParam(required = false) Long developerId,
                       Principal principal, Model model) {
        User me = userService.getByUsername(principal.getName());
                       if (me.getRole() == Role.DEVELOPER) {
                           model.addAttribute("tickets", ticketService.searchVisibleTo(q, status, priority, category, me));
                       } else {
                           model.addAttribute("tickets", ticketService.search(q, status, priority, category, developerId));
                       }
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("priority", priority);
        model.addAttribute("category", category);
        model.addAttribute("developerId", developerId);
        model.addAttribute("developers", userService.activeDevelopers());
        addEnums(model);
        return "tickets";
    }

    // ---------------------------------------------------------------- report a bug

    @GetMapping("/new")
    public String newForm(Model model) {
        TicketForm form = new TicketForm();
        form.setPriority(Priority.MEDIUM);
        form.setSeverity(Severity.MODERATE);
        model.addAttribute("ticketForm", form);
        prepareForm(model, false, null);
        return "ticket-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("ticketForm") TicketForm form, BindingResult result,
                         Principal principal, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepareForm(model, false, null);
            return "ticket-form";
        }
        User me = userService.getByUsername(principal.getName());
        if (me.getRole() != Role.ADMIN) form.setAssignedToId(null);   // only admins assign
        try {
            BugTicket ticket = ticketService.create(form, me);
            redirect.addFlashAttribute("success", "Ticket " + ticket.getTicketId() + " was reported successfully.");
            return "redirect:/tickets/" + ticket.getId();
        } catch (IllegalArgumentException ex) {
            result.reject("ticket.error", ex.getMessage());
            prepareForm(model, false, null);
            return "ticket-form";
        }
    }

    // ---------------------------------------------------------------- ticket details

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Principal principal, Model model) {
        User me = userService.getByUsername(principal.getName());
        BugTicket ticket = ticketService.getForViewer(id, me);
        model.addAttribute("ticket", ticket);
        model.addAttribute("comments", ticketService.commentsFor(ticket));
        model.addAttribute("history", ticketService.historyFor(ticket));
        model.addAttribute("developers", userService.activeDevelopers());
        model.addAttribute("canManage", me.getRole() == Role.ADMIN);
        model.addAttribute("canUpdateStatus", ticketService.canUpdateStatus(ticket, me));
        model.addAttribute("nextStatuses", ticket.getStatus().allowedNext());
        return "ticket-detail";
    }

    // ---------------------------------------------------------------- edit (admin)

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editForm(@PathVariable Long id, Model model) {
        BugTicket ticket = ticketService.getById(id);
        model.addAttribute("ticketForm", TicketForm.from(ticket));
        prepareForm(model, true, ticket);
        return "ticket-form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String edit(@PathVariable Long id, @Valid @ModelAttribute("ticketForm") TicketForm form,
                       BindingResult result, Principal principal, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepareForm(model, true, ticketService.getById(id));
            return "ticket-form";
        }
        User me = userService.getByUsername(principal.getName());
        try {
            ticketService.update(id, form, me);
            redirect.addFlashAttribute("success", "Ticket updated.");
            return "redirect:/tickets/" + id;
        } catch (IllegalArgumentException ex) {
            result.reject("ticket.error", ex.getMessage());
            prepareForm(model, true, ticketService.getById(id));
            return "ticket-form";
        }
    }

    // ---------------------------------------------------------------- delete (admin)

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        ticketService.delete(id);
        redirect.addFlashAttribute("success", "Ticket deleted.");
        return "redirect:/tickets";
    }

    // ---------------------------------------------------------------- assign (admin)

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public String assign(@PathVariable Long id, @RequestParam(required = false) Long developerId,
                         Principal principal, RedirectAttributes redirect) {
        User me = userService.getByUsername(principal.getName());
        try {
            ticketService.assign(id, developerId, me);
            redirect.addFlashAttribute("success", "Assignment updated.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/tickets/" + id;
    }

    // ---------------------------------------------------------------- status and comments

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam Status status,
                               Principal principal, RedirectAttributes redirect) {
        User me = userService.getByUsername(principal.getName());
        try {
            ticketService.changeStatus(id, status, me);
            redirect.addFlashAttribute("success", "Status changed to " + status.getLabel() + ".");
        } catch (IllegalStateException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/tickets/" + id;
    }

    @PostMapping("/{id}/comments")
    public String addComment(@PathVariable Long id, @RequestParam String message,
                             Principal principal, RedirectAttributes redirect) {
        User me = userService.getByUsername(principal.getName());
        try {
            ticketService.addComment(id, message, me);
            redirect.addFlashAttribute("success", "Comment added.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/tickets/" + id;
    }

    // ---------------------------------------------------------------- helpers

    private void addEnums(Model model) {
        model.addAttribute("statuses", Status.values());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("categories", Category.values());
        model.addAttribute("severities", Severity.values());
    }

    private void prepareForm(Model model, boolean editing, BugTicket ticket) {
        addEnums(model);
        model.addAttribute("developers", userService.activeDevelopers());
        model.addAttribute("editing", editing);
        model.addAttribute("ticket", ticket);
    }
}
