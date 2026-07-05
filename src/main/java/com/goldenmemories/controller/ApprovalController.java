package com.goldenmemories.controller;

import com.goldenmemories.model.ApprovalForm;
import com.goldenmemories.model.ApprovalRecord;
import com.goldenmemories.model.Project;
import com.goldenmemories.model.User;
import com.goldenmemories.security.SecurityUtils;
import com.goldenmemories.service.ApprovalService;
import com.goldenmemories.service.ProjectService;
import com.goldenmemories.service.StoryService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/project/{projectId}/approval")
public class ApprovalController {

    private final ApprovalService approvalService;
    private final ProjectService projectService;
    private final StoryService storyService;
    private final SecurityUtils securityUtils;

    public ApprovalController(ApprovalService approvalService,
                               ProjectService projectService,
                               StoryService storyService,
                               SecurityUtils securityUtils) {
        this.approvalService = approvalService;
        this.projectService = projectService;
        this.storyService = storyService;
        this.securityUtils = securityUtils;
    }

    // ── Submit a draft for review ────────────────────────────────────────────

    @GetMapping("/submit")
    public String submitForm(@AuthenticationPrincipal Object principal,
                              @PathVariable Long projectId, Model model) {
        User user = resolveUser(principal);
        Project project = resolveProject(projectId, user);

        long approvedCount = storyService.listByProject(project).stream()
            .filter(s -> s.getStatus() == com.goldenmemories.model.StoryEntry.Status.APPROVED)
            .count();

        // Derive the next version label from existing approval count
        long nextVersion = approvalService.historyFor(project).size() + 1;

        model.addAttribute("project", project);
        model.addAttribute("approvedStoryCount", approvedCount);
        model.addAttribute("nextVersion", "v" + nextVersion);
        model.addAttribute("approvalHistory", approvalService.historyFor(project));
        return "approval/submit";
    }

    @PostMapping("/submit")
    public String submitDraft(@AuthenticationPrincipal Object principal,
                               @PathVariable Long projectId,
                               @RequestParam String draftVersion,
                               RedirectAttributes redirectAttributes) {
        User user = resolveUser(principal);
        Project project = resolveProject(projectId, user);

        approvalService.submitDraft(project, draftVersion, user);
        redirectAttributes.addFlashAttribute("successMessage",
            "Draft " + draftVersion + " submitted for review.");
        return "redirect:/project/" + projectId + "/approval/review";
    }

    // ── Review a pending draft ───────────────────────────────────────────────

    @GetMapping("/review")
    public String reviewForm(@AuthenticationPrincipal Object principal,
                              @PathVariable Long projectId, Model model) {
        User user = resolveUser(principal);
        Project project = resolveProject(projectId, user);

        model.addAttribute("project", project);
        model.addAttribute("latestApproval",
            approvalService.latestApproval(project).orElse(null));
        model.addAttribute("approvalHistory",
            approvalService.historyFor(project));
        model.addAttribute("stories", storyService.listByProject(project));

        if (!model.containsAttribute("approvalForm")) {
            model.addAttribute("approvalForm", new ApprovalForm());
        }
        model.addAttribute("decisions", new ApprovalRecord.Decision[]{
            ApprovalRecord.Decision.APPROVED,
            ApprovalRecord.Decision.REVISION_REQUESTED,
            ApprovalRecord.Decision.REJECTED
        });
        return "approval/review";
    }

    @PostMapping("/{approvalId}/decide")
    public String recordDecision(@AuthenticationPrincipal Object principal,
                                  @PathVariable Long projectId,
                                  @PathVariable Long approvalId,
                                  @Valid @ModelAttribute("approvalForm") ApprovalForm form,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.approvalForm", bindingResult);
            redirectAttributes.addFlashAttribute("approvalForm", form);
            return "redirect:/project/" + projectId + "/approval/review";
        }

        User user = resolveUser(principal);
        approvalService.recordDecision(approvalId, form.getDecision(),
            form.getComments(), user);

        String msg = switch (form.getDecision()) {
            case APPROVED -> "Draft approved. Project moved to publishing.";
            case REVISION_REQUESTED -> "Revision requested. Project moved back to editing.";
            case REJECTED -> "Draft rejected. Project moved back to editing.";
            default -> "Decision recorded.";
        };

        redirectAttributes.addFlashAttribute("successMessage", msg);
        return "redirect:/project/" + projectId + "/approval/review";
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private User resolveUser(Object principal) {
        return securityUtils.currentUser(principal)
            .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    private Project resolveProject(Long projectId, User user) {
        return projectService.findByIdAndOwner(projectId, user)
            .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));
    }
}
