package com.bugtracker.controller;

import com.bugtracker.entity.User;
import com.bugtracker.service.DashboardService;
import com.bugtracker.service.UserService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserService userService;

    public DashboardController(DashboardService dashboardService, UserService userService) {
        this.dashboardService = dashboardService;
        this.userService = userService;
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("stats", dashboardService.adminStats());
        return "admin-dashboard";
    }

    @GetMapping("/developer/dashboard")
    public String developerDashboard(Principal principal, Model model) {
        User me = userService.getByUsername(principal.getName());
        model.addAttribute("stats", dashboardService.developerStats(me));
        return "developer-dashboard";
    }
}
