package com.goldenmemories.controller;

import com.goldenmemories.model.PhotoAsset;
import com.goldenmemories.model.Project;
import com.goldenmemories.model.User;
import com.goldenmemories.security.SecurityUtils;
import com.goldenmemories.service.ProjectService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/project/{projectId}/media")
public class MediaController {

    private final ProjectService projectService;
    private final SecurityUtils securityUtils;

    public MediaController(ProjectService projectService, SecurityUtils securityUtils) {
        this.projectService = projectService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String vault(@AuthenticationPrincipal Object principal,
                        @PathVariable("projectId") Long projectId,
                        Model model) {
        User user = securityUtils.currentUser(principal)
            .orElseThrow(() -> new IllegalStateException("User not found"));

        Project project = projectService.findByIdAndOwner(projectId, user)
            .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));

        List<PhotoAsset> photos = new ArrayList<>(project.getPhotos());
        Map<PhotoAsset.RestorationStatus, Long> counts = photos.stream()
            .collect(Collectors.groupingBy(PhotoAsset::getRestorationStatus, Collectors.counting()));

        model.addAttribute("project", project);
        model.addAttribute("photos", photos);
        model.addAttribute("photoCounts", counts);
        return "project/media";
    }

    @GetMapping("/{photoId}/edit")
    @Transactional(readOnly = true)
    public String edit(@AuthenticationPrincipal Object principal,
                       @PathVariable("projectId") Long projectId,
                       @PathVariable("photoId") Long photoId,
                       Model model) {
        User user = securityUtils.currentUser(principal)
            .orElseThrow(() -> new IllegalStateException("User not found"));

        Project project = projectService.findByIdAndOwner(projectId, user)
            .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));

        PhotoAsset photo = loadPhoto(project, photoId);

        model.addAttribute("project", project);
        model.addAttribute("photo", photo);
        return "project/photo-edit";
    }

    @PostMapping
    @Transactional
    public String upload(@AuthenticationPrincipal Object principal,
                         @PathVariable("projectId") Long projectId,
                         @RequestParam("photoFile") MultipartFile photoFile,
                         @RequestParam(value = "caption", required = false) String caption,
                         @RequestParam(value = "chapterTag", required = false) String chapterTag,
                         @RequestParam(value = "restorationStatus", defaultValue = "ORIGINAL")
                         PhotoAsset.RestorationStatus restorationStatus,
                         RedirectAttributes redirectAttributes) {
        User user = securityUtils.currentUser(principal)
            .orElseThrow(() -> new IllegalStateException("User not found"));

        Project project = projectService.findByIdAndOwner(projectId, user)
            .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));

        List<String> validationErrors = new ArrayList<>();
        if (photoFile.isEmpty()) {
            validationErrors.add("Please choose a photo before uploading.");
        } else {
            String contentType = photoFile.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                validationErrors.add("Only image files (JPEG, PNG, GIF, WEBP, etc.) are allowed.");
            }
            if (photoFile.getSize() > 5 * 1024 * 1024) { // 5MB limit
                validationErrors.add("Photo file size must be under 5 MB.");
            }
        }

        if (caption != null && caption.length() > 255) {
            validationErrors.add("Caption must not exceed 255 characters.");
        }

        if (chapterTag != null && chapterTag.length() > 100) {
            validationErrors.add("Chapter tag must not exceed 100 characters.");
        }

        if (!validationErrors.isEmpty()) {
            redirectAttributes.addFlashAttribute("validationErrors", validationErrors);
            redirectAttributes.addFlashAttribute("prevCaption", caption);
            redirectAttributes.addFlashAttribute("prevChapterTag", chapterTag);
            redirectAttributes.addFlashAttribute("prevRestorationStatus", restorationStatus);
            return "redirect:/project/" + projectId + "/media";
        }

        try {
            String storedPath = storePhotoFile(projectId, photoFile);
            String originalFilename = photoFile.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) {
                originalFilename = "photo";
            }

            projectService.addPhotoAsset(project, originalFilename, storedPath, caption, chapterTag, restorationStatus);
            redirectAttributes.addFlashAttribute("successMessage", "Photo uploaded successfully.");
        } catch (IOException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "We could not save that photo. Please try again.");
        }

        return "redirect:/project/" + projectId + "/media";
    }

    @PostMapping("/{photoId}/status")
    @Transactional
    public String updateStatus(@AuthenticationPrincipal Object principal,
                               @PathVariable("projectId") Long projectId,
                               @PathVariable("photoId") Long photoId,
                               @RequestParam("restorationStatus")
                               PhotoAsset.RestorationStatus restorationStatus,
                               RedirectAttributes redirectAttributes) {
        User user = securityUtils.currentUser(principal)
            .orElseThrow(() -> new IllegalStateException("User not found"));

        Project project = projectService.findByIdAndOwner(projectId, user)
            .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));
        PhotoAsset photo = loadPhoto(project, photoId);

        projectService.updatePhotoAsset(photo, photo.getCaption(), photo.getChapterTag(), restorationStatus);
        redirectAttributes.addFlashAttribute("successMessage", "Photo status updated.");
        return "redirect:/project/" + projectId + "/media";
    }

    @PostMapping("/{photoId}/edit")
    @Transactional
    public String updatePhoto(@AuthenticationPrincipal Object principal,
                              @PathVariable("projectId") Long projectId,
                              @PathVariable("photoId") Long photoId,
                              @RequestParam(value = "caption", required = false) String caption,
                              @RequestParam(value = "chapterTag", required = false) String chapterTag,
                              @RequestParam("restorationStatus") PhotoAsset.RestorationStatus restorationStatus,
                              RedirectAttributes redirectAttributes) {
        User user = securityUtils.currentUser(principal)
            .orElseThrow(() -> new IllegalStateException("User not found"));

        Project project = projectService.findByIdAndOwner(projectId, user)
            .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));
        PhotoAsset photo = loadPhoto(project, photoId);

        List<String> validationErrors = new ArrayList<>();
        if (caption != null && caption.length() > 255) {
            validationErrors.add("Caption must not exceed 255 characters.");
        }

        if (chapterTag != null && chapterTag.length() > 100) {
            validationErrors.add("Chapter tag must not exceed 100 characters.");
        }

        if (!validationErrors.isEmpty()) {
            redirectAttributes.addFlashAttribute("validationErrors", validationErrors);
            return "redirect:/project/" + projectId + "/media/" + photoId + "/edit";
        }

        projectService.updatePhotoAsset(photo, caption, chapterTag, restorationStatus);
        redirectAttributes.addFlashAttribute("successMessage", "Photo details saved.");
        return "redirect:/project/" + projectId + "/media";
    }

    @PostMapping("/{photoId}/delete")
    @Transactional
    public String deletePhoto(@AuthenticationPrincipal Object principal,
                              @PathVariable("projectId") Long projectId,
                              @PathVariable("photoId") Long photoId,
                              RedirectAttributes redirectAttributes) {
        User user = securityUtils.currentUser(principal)
            .orElseThrow(() -> new IllegalStateException("User not found"));

        Project project = projectService.findByIdAndOwner(projectId, user)
            .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));
        PhotoAsset photo = loadPhoto(project, photoId);

        try {
            Files.deleteIfExists(Path.of(photo.getStoragePath()));
            if (photo.getRestoredStoragePath() != null && !photo.getRestoredStoragePath().isBlank()) {
                Files.deleteIfExists(Path.of(photo.getRestoredStoragePath()));
            }
        } catch (IOException ignored) {
            // If the file is already gone, the database record still needs to be removed.
        }

        projectService.deletePhotoAsset(photo);
        redirectAttributes.addFlashAttribute("successMessage", "Photo deleted.");
        return "redirect:/project/" + projectId + "/media";
    }

    private String storePhotoFile(Long projectId, MultipartFile photoFile) throws IOException {
        Path uploadDir = Path.of("uploads", "project-" + projectId);
        Files.createDirectories(uploadDir);

        String originalName = photoFile.getOriginalFilename();
        String safeName = sanitizeFileName(originalName == null || originalName.isBlank() ? "photo" : originalName);
        String storedFileName = UUID.randomUUID() + "-" + safeName;
        Path targetFile = uploadDir.resolve(storedFileName);

        Files.copy(photoFile.getInputStream(), targetFile);
        return uploadDir.resolve(storedFileName).toString().replace('\\', '/');
    }

    private String sanitizeFileName(String fileName) {
        String cleaned = Path.of(fileName).getFileName().toString();
        return cleaned.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private PhotoAsset loadPhoto(Project project, Long photoId) {
        return project.getPhotos().stream()
            .filter(photo -> photoId.equals(photo.getId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Photo not found or access denied"));
    }
}
