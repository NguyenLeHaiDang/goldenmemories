package com.goldenmemories.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.goldenmemories.model.FeatureCard;
import com.goldenmemories.model.ScreenFlowStep;
import com.goldenmemories.model.UseCase;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("features", List.of(
            new FeatureCard("Hybrid onboarding", "Adult children manage the project while parents tell stories through Zalo-based conversations."),
            new FeatureCard("AI transcription", "Voice notes are converted into text drafts, then refined by editors and review loops."),
            new FeatureCard("Photo restoration", "Old family photos can be uploaded, enhanced, and organized by memoir chapter."),
            new FeatureCard("Hidden QR security", "Sensitive memories can be locked behind password-protected QR access."),
            new FeatureCard("Printing + archive", "The final memoir becomes both a hardcover book and a secure cloud archive.")
        ));

        model.addAttribute("useCases", List.of(
            new UseCase("Adult children", "Register the family profile, choose a package, upload materials, monitor progress, approve the draft, and pay."),
            new UseCase("Parents", "Receive prompts, answer by voice, revisit recordings, and continue the storytelling journey with minimal friction."),
            new UseCase("Editors", "Coordinate questions, process audio, edit manuscript content, restore photos, and submit drafts."),
            new UseCase("AI services", "Transcribe voice, clean transcripts, restore image quality, and support automated layout."),
            new UseCase("Cloud services", "Store original audio, photos, secure data, and protected QR content.")
        ));

        model.addAttribute("flowSteps", List.of(
            new ScreenFlowStep("Landing page", "Explore the brand story, features, and packages."),
            new ScreenFlowStep("Consultation", "Submit a form or request a callback before purchase."),
            new ScreenFlowStep("Registration", "Create an account and connect parent contact details."),
            new ScreenFlowStep("Payment", "Choose a package and activate the project."),
            new ScreenFlowStep("Dashboard", "Track progress, stories, photos, and approvals."),
            new ScreenFlowStep("Publishing", "Export the memoir to print and cloud delivery.")
        ));

        return "index";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/product")
    public String product() {
        return "product";
    }

    @GetMapping("/process")
    public String process() {
        return "process";
    }
}
