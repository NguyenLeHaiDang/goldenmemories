package com.goldenmemories.controller;

import com.goldenmemories.model.User;
import com.goldenmemories.security.SecurityUtils;
import com.goldenmemories.service.ProjectService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final ProjectService projectService;
    private final SecurityUtils securityUtils;

    public DashboardController(ProjectService projectService, SecurityUtils securityUtils) {
        this.projectService = projectService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal Object principal, Model model) {

        User user = securityUtils.currentUser(principal)
            .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));

        model.addAttribute("displayName", user.getFullName());

        projectService.buildDashboardSummary(user).ifPresentOrElse(
            summary -> {
                model.addAttribute("summary", summary);
                model.addAttribute("hasProject", true);
            },
            () -> model.addAttribute("hasProject", false)
        );

        return "dashboard";
    }
}
