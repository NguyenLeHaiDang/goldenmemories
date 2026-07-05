package com.goldenmemories.controller;

import com.goldenmemories.model.StoryEntry;
import com.goldenmemories.service.StoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/editor")
public class EditorController {

    private final StoryService storyService;

    public EditorController(StoryService storyService) {
        this.storyService = storyService;
    }

    /**
     * Editor queue: all stories across all projects that need transcription or editing.
     * In a multi-role system this would be restricted to ADMIN/EDITOR roles.
     * For Phase 3, it's accessible to any authenticated user for review purposes.
     */
    @GetMapping("/queue")
    public String queue(Model model) {
        List<StoryEntry> needsTranscription =
            storyService.listByStatus(StoryEntry.Status.RECEIVED);
        List<StoryEntry> needsEditing =
            storyService.listByStatus(StoryEntry.Status.TRANSCRIBED);
        List<StoryEntry> awaitingApproval =
            storyService.listByStatus(StoryEntry.Status.EDITED);

        model.addAttribute("needsTranscription", needsTranscription);
        model.addAttribute("needsEditing", needsEditing);
        model.addAttribute("awaitingApproval", awaitingApproval);
        return "editor/queue";
    }
}
