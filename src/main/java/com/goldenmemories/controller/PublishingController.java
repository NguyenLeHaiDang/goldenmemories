package com.goldenmemories.controller;

import com.goldenmemories.model.ApprovalRecord;
import com.goldenmemories.model.Project;
import com.goldenmemories.model.User;
import com.goldenmemories.security.SecurityUtils;
import com.goldenmemories.service.ApprovalService;
import com.goldenmemories.service.ProjectService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/project/{projectId}/publish")
public class PublishingController {

    private final ProjectService projectService;
    private final ApprovalService approvalService;
    private final SecurityUtils securityUtils;

    public PublishingController(ProjectService projectService,
                                ApprovalService approvalService,
                                SecurityUtils securityUtils) {
        this.projectService = projectService;
        this.approvalService = approvalService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String handoff(@AuthenticationPrincipal Object principal,
                          @PathVariable("projectId") Long projectId,
                          Model model) {
        User user = securityUtils.currentUser(principal)
            .orElseThrow(() -> new IllegalStateException("User not found"));

        Project project = projectService.findByIdAndOwner(projectId, user)
            .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));

        List<ApprovalRecord> approvals = new ArrayList<>(approvalService.historyFor(project));

        model.addAttribute("project", project);
        model.addAttribute("approvals", approvals);
        model.addAttribute("latestApproval", approvalService.latestApproval(project).orElse(null));
        model.addAttribute("photoCount", project.getPhotos().size());
        model.addAttribute("storyCount", project.getStories().size());
        return "project/publish";
    }

    @GetMapping("/export")
    @Transactional(readOnly = true)
    public String export(@AuthenticationPrincipal Object principal,
                         @PathVariable("projectId") Long projectId,
                         Model model) {
        User user = securityUtils.currentUser(principal)
            .orElseThrow(() -> new IllegalStateException("User not found"));

        Project project = projectService.findByIdAndOwner(projectId, user)
            .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));

        List<ApprovalRecord> approvals = new ArrayList<>(approvalService.historyFor(project));

        model.addAttribute("project", project);
        model.addAttribute("approvals", approvals);
        model.addAttribute("latestApproval", approvalService.latestApproval(project).orElse(null));
        model.addAttribute("photoCount", project.getPhotos().size());
        model.addAttribute("storyCount", project.getStories().size());
        model.addAttribute("readyForExport", project.getCurrentPhase() == Project.Phase.PUBLISHING
            || project.getCurrentPhase() == Project.Phase.COMPLETED);
        return "project/export";
    }

    @PostMapping("/complete")
    @Transactional
    public String complete(@AuthenticationPrincipal Object principal,
                           @PathVariable("projectId") Long projectId,
                           RedirectAttributes redirectAttributes) {
        User user = securityUtils.currentUser(principal)
            .orElseThrow(() -> new IllegalStateException("User not found"));

        Project project = projectService.findByIdAndOwner(projectId, user)
            .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));

        projectService.completeProject(project);
        redirectAttributes.addFlashAttribute("successMessage", "Publishing handoff completed. Project marked as finished.");
        return "redirect:/project/" + projectId + "/publish";
    }
}
