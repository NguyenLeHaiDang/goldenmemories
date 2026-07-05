package com.goldenmemories.controller;

import com.goldenmemories.model.Project;
import com.goldenmemories.model.StoryEditForm;
import com.goldenmemories.model.StoryEntry;
import com.goldenmemories.model.StoryPromptForm;
import com.goldenmemories.model.TranscriptForm;
import com.goldenmemories.model.User;
import com.goldenmemories.security.SecurityUtils;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/project/{projectId}/story")
public class StoryController {

    private final StoryService storyService;
    private final ProjectService projectService;
    private final SecurityUtils securityUtils;

    public StoryController(StoryService storyService,
                            ProjectService projectService,
                            SecurityUtils securityUtils) {
        this.storyService = storyService;
        this.projectService = projectService;
        this.securityUtils = securityUtils;
    }

    // ── Story list for a project ─────────────────────────────────────────────

    @GetMapping
    public String list(@AuthenticationPrincipal Object principal,
                       @PathVariable Long projectId, Model model) {
        User user = resolveUser(principal);
        Project project = resolveProject(projectId, user);

        model.addAttribute("project", project);
        model.addAttribute("stories", storyService.listByProject(project));
        model.addAttribute("storyPromptForm", new StoryPromptForm());
        model.addAttribute("lifeStages", StoryEntry.LifeStage.values());
        return "story/list";
    }

    // ── Add prompt ───────────────────────────────────────────────────────────

    @PostMapping("/prompt")
    public String addPrompt(@AuthenticationPrincipal Object principal,
                             @PathVariable Long projectId,
                             @Valid @ModelAttribute("storyPromptForm") StoryPromptForm form,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        User user = resolveUser(principal);
        Project project = resolveProject(projectId, user);

        if (bindingResult.hasErrors()) {
            model.addAttribute("project", project);
            model.addAttribute("stories", storyService.listByProject(project));
            model.addAttribute("lifeStages", StoryEntry.LifeStage.values());
            return "story/list";
        }

        // Advance project to STORY_COLLECTION if still on ONBOARDING
        if (project.getCurrentPhase() == Project.Phase.ONBOARDING) {
            project.setCurrentPhase(Project.Phase.STORY_COLLECTION);
            projectService.findByIdAndOwner(projectId, user); // load managed instance
        }

        storyService.addPrompt(project, form.getPromptQuestion(), form.getLifeStage());
        redirectAttributes.addFlashAttribute("successMessage", "Prompt added.");
        return "redirect:/project/" + projectId + "/story";
    }

    // ── Submit transcript ────────────────────────────────────────────────────

    @GetMapping("/{storyId}/transcript")
    public String transcriptForm(@AuthenticationPrincipal Object principal,
                                  @PathVariable Long projectId,
                                  @PathVariable Long storyId,
                                  Model model) {
        User user = resolveUser(principal);
        Project project = resolveProject(projectId, user);
        StoryEntry story = resolveStory(storyId, project);

        model.addAttribute("project", project);
        model.addAttribute("story", story);
        if (!model.containsAttribute("transcriptForm")) {
            TranscriptForm form = new TranscriptForm();
            form.setRawTranscript(story.getRawTranscript());
            model.addAttribute("transcriptForm", form);
        }
        return "story/transcript";
    }

    @PostMapping("/{storyId}/transcript")
    public String submitTranscript(@AuthenticationPrincipal Object principal,
                                    @PathVariable Long projectId,
                                    @PathVariable Long storyId,
                                    @Valid @ModelAttribute("transcriptForm") TranscriptForm form,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes) {
        User user = resolveUser(principal);
        Project project = resolveProject(projectId, user);
        StoryEntry story = resolveStory(storyId, project);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.transcriptForm", bindingResult);
            redirectAttributes.addFlashAttribute("transcriptForm", form);
            return "redirect:/project/" + projectId + "/story/" + storyId + "/transcript";
        }

        storyService.submitTranscript(story.getId(), form.getRawTranscript());
        redirectAttributes.addFlashAttribute("successMessage", "Transcript saved.");
        return "redirect:/project/" + projectId + "/story";
    }

    // ── Edit story ───────────────────────────────────────────────────────────

    @GetMapping("/{storyId}/edit")
    public String editForm(@AuthenticationPrincipal Object principal,
                            @PathVariable Long projectId,
                            @PathVariable Long storyId,
                            Model model) {
        User user = resolveUser(principal);
        Project project = resolveProject(projectId, user);
        StoryEntry story = resolveStory(storyId, project);

        model.addAttribute("project", project);
        model.addAttribute("story", story);
        if (!model.containsAttribute("storyEditForm")) {
            StoryEditForm form = new StoryEditForm();
            form.setEditedContent(
                story.getEditedContent() != null ? story.getEditedContent() : story.getRawTranscript());
            form.setEditorNotes(story.getEditorNotes());
            model.addAttribute("storyEditForm", form);
        }
        return "story/edit";
    }

    @PostMapping("/{storyId}/edit")
    public String saveEdit(@AuthenticationPrincipal Object principal,
                            @PathVariable Long projectId,
                            @PathVariable Long storyId,
                            @Valid @ModelAttribute("storyEditForm") StoryEditForm form,
                            BindingResult bindingResult,
                            RedirectAttributes redirectAttributes) {
        User user = resolveUser(principal);
        Project project = resolveProject(projectId, user);
        StoryEntry story = resolveStory(storyId, project);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.storyEditForm", bindingResult);
            redirectAttributes.addFlashAttribute("storyEditForm", form);
            return "redirect:/project/" + projectId + "/story/" + storyId + "/edit";
        }

        storyService.saveEdit(story.getId(), form.getEditedContent(), form.getEditorNotes());

        // Advance to EDITING phase if still in STORY_COLLECTION
        if (project.getCurrentPhase() == Project.Phase.STORY_COLLECTION) {
            project.setCurrentPhase(Project.Phase.EDITING);
        }

        redirectAttributes.addFlashAttribute("successMessage", "Story updated successfully.");
        return "redirect:/project/" + projectId + "/story";
    }

    // ── Approve / request revision ───────────────────────────────────────────

    @PostMapping("/{storyId}/approve")
    public String approve(@AuthenticationPrincipal Object principal,
                           @PathVariable Long projectId,
                           @PathVariable Long storyId,
                           RedirectAttributes redirectAttributes) {
        User user = resolveUser(principal);
        Project project = resolveProject(projectId, user);
        StoryEntry story = resolveStory(storyId, project);

        storyService.approveStory(story.getId());
        redirectAttributes.addFlashAttribute("successMessage", "Story approved.");
        return "redirect:/project/" + projectId + "/story";
    }

    @PostMapping("/{storyId}/revise")
    public String revise(@AuthenticationPrincipal Object principal,
                          @PathVariable Long projectId,
                          @PathVariable Long storyId,
                          RedirectAttributes redirectAttributes) {
        User user = resolveUser(principal);
        Project project = resolveProject(projectId, user);
        StoryEntry story = resolveStory(storyId, project);

        storyService.requestRevision(story.getId());
        redirectAttributes.addFlashAttribute("successMessage", "Story sent back for revision.");
        return "redirect:/project/" + projectId + "/story";
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

    private StoryEntry resolveStory(Long storyId, Project project) {
        return storyService.findById(storyId)
            .filter(s -> s.getProject().getId().equals(project.getId()))
            .orElseThrow(() -> new IllegalArgumentException("Story not found in this project"));
    }
}
