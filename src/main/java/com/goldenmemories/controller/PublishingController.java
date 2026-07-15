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
import com.goldenmemories.model.PhotoAsset;
import com.goldenmemories.model.StoryEntry;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import jakarta.servlet.http.HttpServletResponse;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.nio.file.Files;
import java.nio.file.Path;

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
        model.addAttribute("qrProtectedCount", project.getStories().stream().filter(StoryEntry::isQrProtected).count());
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
        return "redirect:/project/" + projectId + "/publish/complete";
    }

    /** Completion summary page — shown after a project is marked complete. */
    @GetMapping("/complete")
    @Transactional(readOnly = true)
    public String completeSummary(@AuthenticationPrincipal Object principal,
                                  @PathVariable("projectId") Long projectId,
                                  Model model) {
        User user = securityUtils.currentUser(principal)
            .orElseThrow(() -> new IllegalStateException("User not found"));

        Project project = projectService.findByIdAndOwner(projectId, user)
            .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));

        model.addAttribute("project", project);
        model.addAttribute("photoCount", project.getPhotos().size());
        model.addAttribute("storyCount", project.getStories().size());
        model.addAttribute("stories", project.getStories());
        model.addAttribute("qrProtectedCount", project.getStories().stream().filter(StoryEntry::isQrProtected).count());
        model.addAttribute("latestApproval", approvalService.latestApproval(project).orElse(null));
        return "project/complete";
    }

    /** Save print handoff details (vendor, address, notes). */
    @PostMapping("/print-handoff")
    @Transactional
    public String savePrintHandoff(@AuthenticationPrincipal Object principal,
                                   @PathVariable("projectId") Long projectId,
                                   @org.springframework.web.bind.annotation.RequestParam(value = "printVendorName", defaultValue = "") String vendorName,
                                   @org.springframework.web.bind.annotation.RequestParam(value = "printDeliveryAddress", defaultValue = "") String deliveryAddress,
                                   @org.springframework.web.bind.annotation.RequestParam(value = "printNotes", defaultValue = "") String printNotes,
                                   RedirectAttributes redirectAttributes) {
        User user = securityUtils.currentUser(principal)
            .orElseThrow(() -> new IllegalStateException("User not found"));

        Project project = projectService.findByIdAndOwner(projectId, user)
            .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));

        projectService.saveHandoffDetails(project,
            vendorName.isBlank() ? null : vendorName.strip(),
            deliveryAddress.isBlank() ? null : deliveryAddress.strip(),
            printNotes.isBlank() ? null : printNotes.strip());

        redirectAttributes.addFlashAttribute("successMessage", "Print handoff details saved.");
        return "redirect:/project/" + projectId + "/publish";
    }

    /** Save cloud archive destination details. */
    @PostMapping("/archive")
    @Transactional
    public String saveArchive(@AuthenticationPrincipal Object principal,
                              @PathVariable("projectId") Long projectId,
                              @org.springframework.web.bind.annotation.RequestParam(value = "archiveUrl", defaultValue = "") String archiveUrl,
                              @org.springframework.web.bind.annotation.RequestParam(value = "archiveProvider", defaultValue = "") String archiveProvider,
                              @org.springframework.web.bind.annotation.RequestParam(value = "archiveNotes", defaultValue = "") String archiveNotes,
                              RedirectAttributes redirectAttributes) {
        User user = securityUtils.currentUser(principal)
            .orElseThrow(() -> new IllegalStateException("User not found"));

        Project project = projectService.findByIdAndOwner(projectId, user)
            .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));

        projectService.saveArchiveDetails(project,
            archiveUrl.isBlank() ? null : archiveUrl.strip(),
            archiveProvider.isBlank() ? null : archiveProvider.strip(),
            archiveNotes.isBlank() ? null : archiveNotes.strip());

        redirectAttributes.addFlashAttribute("successMessage", "Cloud archive details saved.");
        return "redirect:/project/" + projectId + "/publish";
    }

    @GetMapping("/export/manuscript")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadManuscript(@AuthenticationPrincipal Object principal,
                                                     @PathVariable("projectId") Long projectId) {
        User user = securityUtils.currentUser(principal)
            .orElseThrow(() -> new IllegalStateException("User not found"));

        Project project = projectService.findByIdAndOwner(projectId, user)
            .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));

        StringBuilder sb = new StringBuilder();
        sb.append("# Memoir Manuscript: ").append(project.getTitle()).append("\n");
        sb.append("Generated on: ").append(java.time.Instant.now().toString()).append("\n");
        sb.append("Package: ").append(project.getSelectedPackage()).append("\n");
        sb.append("Owner: ").append(project.getOwner().getFullName()).append(" (").append(project.getOwner().getEmail()).append(")\n\n");
        sb.append("---\n\n");

        List<StoryEntry> stories = project.getStories();
        if (stories.isEmpty()) {
            sb.append("No stories recorded in this project yet.\n");
        } else {
            for (StoryEntry story : stories) {
                sb.append("## [").append(story.getLifeStage()).append("] ").append(story.getPromptQuestion()).append("\n\n");
                sb.append("**Status:** ").append(story.getStatus()).append("\n\n");
                
                String content = story.getEditedContent();
                if (content == null || content.isBlank()) {
                    content = story.getRawTranscript();
                }
                
                if (content == null || content.isBlank()) {
                    sb.append("*Awaiting response/transcript.*\n\n");
                } else {
                    sb.append(content).append("\n\n");
                }
                if (story.getEditorNotes() != null && !story.getEditorNotes().isBlank()) {
                    sb.append("> **Editor Notes:** ").append(story.getEditorNotes()).append("\n\n");
                }
                sb.append("---\n\n");
            }
        }

        byte[] bytes = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"project_" + projectId + "_manuscript.md\"")
            .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
            .body(bytes);
    }

    @GetMapping("/export/images")
    @Transactional(readOnly = true)
    public void downloadImages(@AuthenticationPrincipal Object principal,
                               @PathVariable("projectId") Long projectId,
                               HttpServletResponse response) throws java.io.IOException {
        User user = securityUtils.currentUser(principal)
            .orElseThrow(() -> new IllegalStateException("User not found"));

        Project project = projectService.findByIdAndOwner(projectId, user)
            .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));

        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"project_" + projectId + "_images.zip\"");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            List<PhotoAsset> photos = project.getPhotos();
            for (PhotoAsset photo : photos) {
                // Add original photo
                if (photo.getStoragePath() != null && !photo.getStoragePath().isBlank()) {
                    Path originalPath = Path.of(photo.getStoragePath());
                    if (Files.exists(originalPath)) {
                        String originalEntryName = "original/" + photo.getId() + "_" + photo.getOriginalFilename();
                        ZipEntry originalEntry = new ZipEntry(originalEntryName);
                        zos.putNextEntry(originalEntry);
                        Files.copy(originalPath, zos);
                        zos.closeEntry();
                    }
                }

                // Add restored photo if it exists
                if (photo.getRestoredStoragePath() != null && !photo.getRestoredStoragePath().isBlank()) {
                    Path restoredPath = Path.of(photo.getRestoredStoragePath());
                    if (Files.exists(restoredPath)) {
                        String restoredEntryName = "restored/" + photo.getId() + "_" + photo.getOriginalFilename();
                        ZipEntry restoredEntry = new ZipEntry(restoredEntryName);
                        zos.putNextEntry(restoredEntry);
                        Files.copy(restoredPath, zos);
                        zos.closeEntry();
                    }
                }
            }
        }
    }

    @GetMapping("/export/manifest")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadManifest(@AuthenticationPrincipal Object principal,
                                                   @PathVariable("projectId") Long projectId) {
        User user = securityUtils.currentUser(principal)
            .orElseThrow(() -> new IllegalStateException("User not found"));

        Project project = projectService.findByIdAndOwner(projectId, user)
            .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));

        StringBuilder sb = new StringBuilder();
        sb.append("==================================================\n");
        sb.append("            KÝ ỨC VÀNG ARCHIVE MANIFEST           \n");
        sb.append("==================================================\n\n");
        sb.append("Project ID:       ").append(project.getId()).append("\n");
        sb.append("Project Title:    ").append(project.getTitle()).append("\n");
        sb.append("Package Tier:     ").append(project.getSelectedPackage()).append("\n");
        sb.append("Current Phase:    ").append(project.getCurrentPhase()).append("\n");
        sb.append("Created At:       ").append(project.getCreatedAt()).append("\n");
        sb.append("Last Updated:     ").append(project.getUpdatedAt()).append("\n\n");

        sb.append("----------------- Owner Details -----------------\n");
        sb.append("Full Name:        ").append(project.getOwner().getFullName()).append("\n");
        sb.append("Email:            ").append(project.getOwner().getEmail()).append("\n");
        sb.append("Phone:            ").append(project.getOwner().getPhone() != null ? project.getOwner().getPhone() : "N/A").append("\n\n");

        sb.append("----------------- Stats Summary -----------------\n");
        sb.append("Total Stories:    ").append(project.getStories().size()).append("\n");
        sb.append("Total Photos:     ").append(project.getPhotos().size()).append("\n");
        long restored = project.getPhotos().stream().filter(p -> p.getRestorationStatus() == PhotoAsset.RestorationStatus.RESTORED).count();
        long processing = project.getPhotos().stream().filter(p -> p.getRestorationStatus() == PhotoAsset.RestorationStatus.PROCESSING).count();
        long original = project.getPhotos().stream().filter(p -> p.getRestorationStatus() == PhotoAsset.RestorationStatus.ORIGINAL).count();
        long skipped = project.getPhotos().stream().filter(p -> p.getRestorationStatus() == PhotoAsset.RestorationStatus.SKIPPED).count();
        sb.append("  - Restored:     ").append(restored).append("\n");
        sb.append("  - Processing:   ").append(processing).append("\n");
        sb.append("  - Original:     ").append(original).append("\n");
        sb.append("  - Skipped:      ").append(skipped).append("\n\n");

        sb.append("----------------- Approval History --------------\n");
        List<ApprovalRecord> approvals = project.getApprovals();
        if (approvals.isEmpty()) {
            sb.append("No approval history recorded.\n");
        } else {
            for (ApprovalRecord approval : approvals) {
                String reviewerName = (approval.getReviewer() != null)
                    ? approval.getReviewer().getFullName() : "N/A";
                String decidedAt = (approval.getDecidedAt() != null)
                    ? approval.getDecidedAt().toString() : "Pending";
                sb.append("- Draft: ").append(approval.getDraftVersion())
                  .append(" | Decision: ").append(approval.getDecision())
                  .append(" | Reviewer: ").append(reviewerName)
                  .append(" | Date: ").append(decidedAt).append("\n");
                if (approval.getComments() != null && !approval.getComments().isBlank()) {
                    sb.append("  Comments: ").append(approval.getComments()).append("\n");
                }
            }
        }

        byte[] bytes = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"project_" + projectId + "_manifest.txt\"")
            .contentType(MediaType.TEXT_PLAIN)
            .body(bytes);
    }
}
