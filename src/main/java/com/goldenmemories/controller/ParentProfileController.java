package com.goldenmemories.controller;

import com.goldenmemories.model.ParentProfile;
import com.goldenmemories.model.ParentProfileForm;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/parent")
public class ParentProfileController {

    private final ProjectService projectService;
    private final SecurityUtils securityUtils;

    public ParentProfileController(ProjectService projectService, SecurityUtils securityUtils) {
        this.projectService = projectService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/profile")
    public String profileForm(@AuthenticationPrincipal Object principal, Model model) {
        User user = securityUtils.currentUser(principal)
            .orElseThrow(() -> new IllegalStateException("User not found"));

        // Pre-fill the form if a profile already exists
        ParentProfileForm form = projectService.findParentProfile(user)
            .map(p -> {
                ParentProfileForm f = new ParentProfileForm();
                f.setParentName(p.getParentName());
                f.setRelation(p.getRelation());
                f.setZaloContact(p.getZaloContact());
                f.setAdditionalPhone(p.getAdditionalPhone());
                f.setNotes(p.getNotes());
                return f;
            })
            .orElseGet(ParentProfileForm::new);

        if (!model.containsAttribute("parentProfileForm")) {
            model.addAttribute("parentProfileForm", form);
        }

        model.addAttribute("hasProfile",
            projectService.findParentProfile(user).isPresent());

        return "parent/profile";
    }

    @PostMapping("/profile")
    public String saveProfile(@AuthenticationPrincipal Object principal,
                               @Valid @ModelAttribute("parentProfileForm") ParentProfileForm form,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("hasProfile", false);
            return "parent/profile";
        }

        User user = securityUtils.currentUser(principal)
            .orElseThrow(() -> new IllegalStateException("User not found"));

        projectService.saveParentProfile(user, form.getParentName(), form.getRelation(),
            form.getZaloContact(), form.getAdditionalPhone(), form.getNotes());

        redirectAttributes.addFlashAttribute("successMessage",
            "Parent profile saved successfully.");
        return "redirect:/dashboard";
    }
}
