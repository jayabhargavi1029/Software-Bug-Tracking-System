package com.bugtracker.controller;

import com.bugtracker.dto.UserForm;
import com.bugtracker.entity.Role;
import com.bugtracker.entity.User;
import com.bugtracker.exception.DuplicateUserException;
import com.bugtracker.service.UserService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** User management - the whole /admin/** area is restricted to administrators in SecurityConfig. */
@Controller
@RequestMapping("/admin/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("userForm", new UserForm());
        prepare(model);
        return "users";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("account", userService.getById(id));
        return "user-details";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("userForm") UserForm form, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                User user = userService.create(form);
                redirect.addFlashAttribute("success", "User '" + user.getUsername() + "' was created.");
                return "redirect:/admin/users";
            } catch (DuplicateUserException ex) {
                result.rejectValue(ex.getField(), "duplicate", ex.getMessage());
            }
        }
        prepare(model);
        return "users";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        User me = userService.getByUsername(principal.getName());
        try {
            User user = userService.toggleActive(id, me);
            redirect.addFlashAttribute("success",
                    "User '" + user.getUsername() + "' is now " + (user.isActive() ? "active." : "deactivated."));
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/remove-access")
    public String removeAccess(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        User me = userService.getByUsername(principal.getName());
        try {
            userService.removeAccess(id, me);
            redirect.addFlashAttribute("success",
                    "Login access removed. The user's identity and work history remain available for accountability.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    private void prepare(Model model) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("roles", Role.values());
    }
}
