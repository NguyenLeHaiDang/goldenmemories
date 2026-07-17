package com.goldenmemories.controller;

import com.goldenmemories.model.ApprovalRecord;
import com.goldenmemories.model.PhotoAsset;
import com.goldenmemories.model.Project;
import com.goldenmemories.model.StoryEntry;
import com.goldenmemories.model.User;
import com.goldenmemories.security.SecurityUtils;
import com.goldenmemories.service.ApprovalService;
import com.goldenmemories.service.ProjectService;
import com.goldenmemories.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
@RequestMapping("/project/{projectId}/publish")
public class PublishingController {

    private final ProjectService projectService;
    private final ApprovalService approvalService;
    private final SecurityUtils securityUtils;
    private final VnPayService vnPayService;

    public PublishingController(ProjectService projectService,
                                ApprovalService approvalService,
                                SecurityUtils securityUtils,
                                VnPayService vnPayService) {
        this.projectService = projectService;
        this.approvalService = approvalService;
        this.securityUtils = securityUtils;
        this.vnPayService = vnPayService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String handoff(@AuthenticationPrincipal Object principal,
                          @PathVariable("projectId") Long projectId,
                          Model model) {
        User user = resolveUser(principal);
        Project project = resolveOwnedProject(projectId, user);

        List<ApprovalRecord> approvals = new ArrayList<>(approvalService.historyFor(project));

        model.addAttribute("project", project);
        model.addAttribute("approvals", approvals);
        model.addAttribute("latestApproval", approvalService.latestApproval(project).orElse(null));
        model.addAttribute("photoCount", project.getPhotos().size());
        model.addAttribute("storyCount", project.getStories().size());
        model.addAttribute("qrProtectedCount", project.getStories().stream().filter(StoryEntry::isQrProtected).count());
        return "project/publish";
    }

    @GetMapping("/payment")
    @Transactional(readOnly = true)
    public String payment(@AuthenticationPrincipal Object principal,
                          @PathVariable("projectId") Long projectId,
                          Model model) {
        User user = resolveUser(principal);
        Project project = resolveOwnedProject(projectId, user);

        long amountVnd = paymentAmountFor(project);
        model.addAttribute("project", project);
        model.addAttribute("storyCount", project.getStories().size());
        model.addAttribute("photoCount", project.getPhotos().size());
        model.addAttribute("latestApproval", approvalService.latestApproval(project).orElse(null));
        model.addAttribute("amountVnd", amountVnd);
        model.addAttribute("amountLabel", formatVnd(amountVnd));
        model.addAttribute("vnpayConfigured", vnPayService.isConfigured());
        model.addAttribute("bankCodes", new String[]{"", "VNPAYQR", "VNBANK", "INTCARD"});
        return "project/payment";
    }

    @PostMapping("/payment")
    public String redirectToVnPayLegacy(@AuthenticationPrincipal Object principal,
                                        @PathVariable("projectId") Long projectId,
                                        @RequestParam(value = "bankCode", defaultValue = "") String bankCode,
                                        @RequestParam(value = "paymentNote", defaultValue = "") String paymentNote,
                                        HttpServletRequest request,
                                        RedirectAttributes redirectAttributes) {
        return initiateVnPayPayment(principal, projectId, bankCode, paymentNote, request, redirectAttributes);
    }

    @PostMapping("/payment/vnpay")
    public String initiateVnPayPayment(@AuthenticationPrincipal Object principal,
                                       @PathVariable("projectId") Long projectId,
                                       @RequestParam(value = "bankCode", defaultValue = "") String bankCode,
                                       @RequestParam(value = "paymentNote", defaultValue = "") String paymentNote,
                                       HttpServletRequest request,
                                       RedirectAttributes redirectAttributes) {
        User user = resolveUser(principal);
        Project project = resolveOwnedProject(projectId, user);

        if (!vnPayService.isConfigured()) {
            redirectAttributes.addFlashAttribute("warningMessage",
                "VNPay chưa được cấu hình. Vui lòng cập nhật mã TMN, secret key và public base URL.");
            return "redirect:/project/" + projectId + "/publish/payment";
        }

        String orderInfo = buildVnPayOrderInfo(project, paymentNote);
        String paymentUrl = vnPayService.buildPaymentUrl(
            project,
            paymentAmountFor(project),
            bankCode,
            orderInfo,
            vnPayService.buildReturnUrl(projectId),
            vnPayService.buildIpnUrl(projectId),
            request.getRemoteAddr()
        );
        return "redirect:" + paymentUrl;
    }

    @GetMapping("/payment/return")
    public String vnPayReturn(@PathVariable("projectId") Long projectId,
                              HttpServletRequest request,
                              Model model) {
        Map<String, String> params = vnPayService.extractParameters(request);
        String outcome = handleVnPayCallback(projectId, params);

        Project project = projectService.findById(projectId).orElse(null);
        model.addAttribute("project", project);
        model.addAttribute("projectId", projectId);
        model.addAttribute("returnStatus", params.getOrDefault("vnp_ResponseCode", ""));
        model.addAttribute("responseMessage", responseMessage(params.getOrDefault("vnp_ResponseCode", "")));
        model.addAttribute("txnRef", params.getOrDefault("vnp_TxnRef", ""));
        model.addAttribute("transactionNo", params.getOrDefault("vnp_TransactionNo", ""));
        model.addAttribute("bankCode", params.getOrDefault("vnp_BankCode", ""));
        model.addAttribute("amountLabel", formatVnd(parseAmount(params.getOrDefault("vnp_Amount", "0")) / 100));
        model.addAttribute("success", "success".equals(outcome));
        model.addAttribute("message", "success".equals(outcome)
            ? "Thanh toán VNPay đã được xác nhận."
            : "Thanh toán chưa hoàn tất hoặc chữ ký không hợp lệ.");
        return "project/payment-result";
    }

    @GetMapping("/payment/ipn")
    public ResponseEntity<Map<String, String>> vnPayIpn(@PathVariable("projectId") Long projectId,
                                                        HttpServletRequest request) {
        Map<String, String> params = vnPayService.extractParameters(request);
        String outcome = handleVnPayCallback(projectId, params);

        Map<String, String> body = new HashMap<>();
        if ("success".equals(outcome)) {
            body.put("RspCode", "00");
            body.put("Message", "Confirm Success");
        } else if ("amount-mismatch".equals(outcome)) {
            body.put("RspCode", "04");
            body.put("Message", "Invalid Amount");
        } else if ("not-found".equals(outcome)) {
            body.put("RspCode", "01");
            body.put("Message", "Order not found");
        } else if ("bad-signature".equals(outcome)) {
            body.put("RspCode", "97");
            body.put("Message", "Invalid signature");
        } else {
            body.put("RspCode", "99");
            body.put("Message", "Unknown error");
        }
        return ResponseEntity.ok(body);
    }

    @GetMapping("/export")
    @Transactional(readOnly = true)
    public String export(@AuthenticationPrincipal Object principal,
                         @PathVariable("projectId") Long projectId,
                         Model model) {
        User user = resolveUser(principal);
        Project project = resolveOwnedProject(projectId, user);

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
        User user = resolveUser(principal);
        Project project = resolveOwnedProject(projectId, user);

        if (!project.isPaymentConfirmed()) {
            redirectAttributes.addFlashAttribute("warningMessage",
                "Vui lòng hoàn tất thanh toán trước khi đánh dấu dự án đã xong.");
            return "redirect:/project/" + projectId + "/publish/payment";
        }

        projectService.completeProject(project);
        redirectAttributes.addFlashAttribute("successMessage",
            "Publishing handoff completed. Project marked as finished.");
        return "redirect:/project/" + projectId + "/publish/complete";
    }

    @GetMapping("/complete")
    @Transactional(readOnly = true)
    public String completeSummary(@AuthenticationPrincipal Object principal,
                                  @PathVariable("projectId") Long projectId,
                                  Model model) {
        User user = resolveUser(principal);
        Project project = resolveOwnedProject(projectId, user);

        model.addAttribute("project", project);
        model.addAttribute("photoCount", project.getPhotos().size());
        model.addAttribute("storyCount", project.getStories().size());
        model.addAttribute("stories", project.getStories());
        model.addAttribute("qrProtectedCount", project.getStories().stream().filter(StoryEntry::isQrProtected).count());
        model.addAttribute("latestApproval", approvalService.latestApproval(project).orElse(null));
        return "project/complete";
    }

    @PostMapping("/print-handoff")
    @Transactional
    public String savePrintHandoff(@AuthenticationPrincipal Object principal,
                                   @PathVariable("projectId") Long projectId,
                                   @RequestParam(value = "printVendorName", defaultValue = "") String vendorName,
                                   @RequestParam(value = "printDeliveryAddress", defaultValue = "") String deliveryAddress,
                                   @RequestParam(value = "printNotes", defaultValue = "") String printNotes,
                                   RedirectAttributes redirectAttributes) {
        User user = resolveUser(principal);
        Project project = resolveOwnedProject(projectId, user);

        projectService.saveHandoffDetails(project,
            vendorName.isBlank() ? null : vendorName.strip(),
            deliveryAddress.isBlank() ? null : deliveryAddress.strip(),
            printNotes.isBlank() ? null : printNotes.strip());

        redirectAttributes.addFlashAttribute("successMessage", "Print handoff details saved.");
        return "redirect:/project/" + projectId + "/publish";
    }

    @PostMapping("/archive")
    @Transactional
    public String saveArchive(@AuthenticationPrincipal Object principal,
                              @PathVariable("projectId") Long projectId,
                              @RequestParam(value = "archiveUrl", defaultValue = "") String archiveUrl,
                              @RequestParam(value = "archiveProvider", defaultValue = "") String archiveProvider,
                              @RequestParam(value = "archiveNotes", defaultValue = "") String archiveNotes,
                              RedirectAttributes redirectAttributes) {
        User user = resolveUser(principal);
        Project project = resolveOwnedProject(projectId, user);

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
        User user = resolveUser(principal);
        Project project = resolveOwnedProject(projectId, user);

        StringBuilder sb = new StringBuilder();
        sb.append("# Memoir Manuscript: ").append(project.getTitle()).append("\n");
        sb.append("Generated on: ").append(Instant.now()).append("\n");
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
        User user = resolveUser(principal);
        Project project = resolveOwnedProject(projectId, user);

        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"project_" + projectId + "_images.zip\"");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            List<PhotoAsset> photos = project.getPhotos();
            for (PhotoAsset photo : photos) {
                if (photo.getStoragePath() != null && !photo.getStoragePath().isBlank()) {
                    Path originalPath = Path.of(photo.getStoragePath());
                    if (Files.exists(originalPath)) {
                        String originalEntryName = "original/" + photo.getId() + "_" + photo.getOriginalFilename();
                        zos.putNextEntry(new ZipEntry(originalEntryName));
                        Files.copy(originalPath, zos);
                        zos.closeEntry();
                    }
                }

                if (photo.getRestoredStoragePath() != null && !photo.getRestoredStoragePath().isBlank()) {
                    Path restoredPath = Path.of(photo.getRestoredStoragePath());
                    if (Files.exists(restoredPath)) {
                        String restoredEntryName = "restored/" + photo.getId() + "_" + photo.getOriginalFilename();
                        zos.putNextEntry(new ZipEntry(restoredEntryName));
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
        User user = resolveUser(principal);
        Project project = resolveOwnedProject(projectId, user);

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
                String reviewerName = approval.getReviewer() != null ? approval.getReviewer().getFullName() : "N/A";
                String decidedAt = approval.getDecidedAt() != null ? approval.getDecidedAt().toString() : "Pending";
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

    private User resolveUser(Object principal) {
        return securityUtils.currentUser(principal)
            .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    private Project resolveOwnedProject(Long projectId, User user) {
        return projectService.findByIdAndOwner(projectId, user)
            .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));
    }

    private long paymentAmountFor(Project project) {
        return switch (project.getSelectedPackage()) {
            case BASIC -> 2_900_000L;
            case STANDARD -> 4_900_000L;
            case PREMIUM -> 7_900_000L;
        };
    }

    private String formatVnd(long amount) {
        return String.format(java.util.Locale.forLanguageTag("vi-VN"), "%,d", amount)
            .replace(',', '.') + " đ";
    }

    private String buildVnPayOrderInfo(Project project, String paymentNote) {
        String base = "Thanh toan goi " + project.getSelectedPackage() + " cho du an " + project.getId();
        if (paymentNote != null && !paymentNote.isBlank()) {
            base = base + " - " + paymentNote.trim();
        }
        return vnPayService.normalizeOrderInfo(base);
    }

    private String handleVnPayCallback(Long projectId, Map<String, String> params) {
        if (!vnPayService.verifySignature(params)) {
            return "bad-signature";
        }

        Project project = projectService.findById(projectId).orElse(null);
        if (project == null) {
            return "not-found";
        }

        long expectedAmount = paymentAmountFor(project);
        long returnedAmount = parseAmount(params.getOrDefault("vnp_Amount", "0")) / 100;
        if (returnedAmount != expectedAmount) {
            return "amount-mismatch";
        }

        String responseCode = params.getOrDefault("vnp_ResponseCode", "");
        if ("00".equals(responseCode) && !project.isPaymentConfirmed()) {
            projectService.recordPayment(project,
                "VNPay",
                params.getOrDefault("vnp_TxnRef", ""),
                params.getOrDefault("vnp_TransactionNo", ""),
                responseCode,
                params.getOrDefault("vnp_BankCode", ""));
        }

        return "00".equals(responseCode) ? "success" : "pending";
    }

    private long parseAmount(String amount) {
        try {
            return Long.parseLong(amount);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private String responseMessage(String responseCode) {
        return switch (responseCode) {
            case "00" -> "Giao dịch thành công.";
            case "01" -> "Giao dịch chưa được xác nhận.";
            case "04" -> "Số tiền thanh toán không khớp.";
            case "97" -> "Chữ ký không hợp lệ.";
            default -> "Vui lòng kiểm tra lại trạng thái giao dịch.";
        };
    }
}
