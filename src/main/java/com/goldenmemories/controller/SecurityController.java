package com.goldenmemories.controller;
import com.goldenmemories.model.Project;
import com.goldenmemories.model.StoryEntry;
import com.goldenmemories.model.User;
import com.goldenmemories.security.SecurityUtils;
import com.goldenmemories.service.ProjectService;
import com.goldenmemories.service.StoryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Handles the QR security panel — lets owners configure per-story QR protection.
 * Phase 4 placeholder: no real encryption yet, just flagging + hint text.
 */
@Controller
@RequestMapping("/project/{projectId}/security")
public class SecurityController {

    private final ProjectService projectService;
    private final StoryService storyService;
    private final SecurityUtils securityUtils;

    public SecurityController(ProjectService projectService,
                              StoryService storyService,
                              SecurityUtils securityUtils) {
        this.projectService = projectService;
        this.storyService = storyService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String securityPanel(@AuthenticationPrincipal Object principal,
                                @PathVariable("projectId") Long projectId,
                                Model model) {
        User user = securityUtils.currentUser(principal)
            .orElseThrow(() -> new IllegalStateException("User not found"));

        Project project = projectService.findByIdAndOwner(projectId, user)
            .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));

        List<StoryEntry> stories = storyService.listByProject(project);

        model.addAttribute("project", project);
        model.addAttribute("stories", stories);
        return "project/security-panel";
    }

    @PostMapping("/story/{storyId}")
    @Transactional
    public String updateStoryQr(@AuthenticationPrincipal Object principal,
                                @PathVariable("projectId") Long projectId,
                                @PathVariable("storyId") Long storyId,
                                @RequestParam(value = "qrProtected", defaultValue = "false") boolean qrProtected,
                                @RequestParam(value = "qrHint", defaultValue = "") String qrHint,
                                RedirectAttributes redirectAttributes) {
        User user = securityUtils.currentUser(principal)
            .orElseThrow(() -> new IllegalStateException("User not found"));

        projectService.findByIdAndOwner(projectId, user)
            .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));

        storyService.updateQrConfig(storyId, qrProtected, qrHint.isBlank() ? null : qrHint.strip());

        redirectAttributes.addFlashAttribute("successMessage",
            "QR security settings updated for the story entry.");
        return "redirect:/project/" + projectId + "/security";
    }
}
