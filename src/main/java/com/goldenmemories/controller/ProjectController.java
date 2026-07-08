package com.goldenmemories.controller;

import com.goldenmemories.model.Project;
import com.goldenmemories.model.ProjectForm;
import com.goldenmemories.model.User;
import com.goldenmemories.security.SecurityUtils;
import com.goldenmemories.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Controller
@RequestMapping("/project")
public class ProjectController {

    private final ProjectService projectService;
    private final SecurityUtils securityUtils;

    public ProjectController(ProjectService projectService, SecurityUtils securityUtils) {
        this.projectService = projectService;
        this.securityUtils = securityUtils;
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @GetMapping("/new")
    public String newProject(@AuthenticationPrincipal Object principal, Model model) {
        if (!model.containsAttribute("projectForm")) {
            model.addAttribute("projectForm", new ProjectForm());
        }
        model.addAttribute("packages", Project.Package.values());
        return "project/create";
    }

    @PostMapping("/new")
    public String createProject(@AuthenticationPrincipal Object principal,
                                @Valid @ModelAttribute("projectForm") ProjectForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("packages", Project.Package.values());
            return "project/create";
        }

        User user = securityUtils.currentUser(principal)
            .orElseThrow(() -> new IllegalStateException("User not found"));

        Project project = projectService.createProject(user, form.getTitle(), form.getSelectedPackage());

        redirectAttributes.addFlashAttribute("successMessage",
            "Project \"" + project.getTitle() + "\" created successfully.");
        return "redirect:/project/" + project.getId();
    }

    // ── View ─────────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public String viewProject(@AuthenticationPrincipal Object principal,
                               @PathVariable("id") Long id,
                               Model model) {
        User user = securityUtils.currentUser(principal)
            .orElseThrow(() -> new IllegalStateException("User not found"));

        Project project = projectService.findByIdAndOwner(id, user)
            .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));

        model.addAttribute("project", project);
        model.addAttribute("stories", new ArrayList<>(project.getStories()));
        model.addAttribute("photos", new ArrayList<>(project.getPhotos()));
        model.addAttribute("approvals", new ArrayList<>(project.getApprovals()));
        return "project/view";
    }
}
