package com.bugtracker.controller;

import java.security.Principal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Login page, role-based redirect after login and the "access denied" page. */
@Controller
public class AuthController {

    @GetMapping("/")
    public String root() {
        return "landing";
    }

    @GetMapping("/login")
    public String login(Principal principal) {
        if (principal != null) return "redirect:/dashboard";   // already signed in
        return "login";
    }

    /** Sends admins and developers to their own dashboard. */
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return admin ? "redirect:/admin/dashboard" : "redirect:/developer/dashboard";
    }

    // RequestMapping (not GetMapping) because Spring Security forwards the original HTTP method here.
    @RequestMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}
