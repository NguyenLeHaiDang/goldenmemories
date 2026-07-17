package com.goldenmemories.service;

import com.goldenmemories.model.ParentProfile;
import com.goldenmemories.model.PhotoAsset;
import com.goldenmemories.model.Project;
import com.goldenmemories.model.User;

import java.util.List;
import java.util.Optional;

public interface ProjectService {

    /** Create a new project owned by the given user. */
    Project createProject(User owner, String title, Project.Package pkg);

    /** List all projects for a user, newest first. */
    List<Project> listProjects(User owner);

    /** Find a project by id, only if it belongs to the given owner. */
    Optional<Project> findByIdAndOwner(Long projectId, User owner);

    /** Find a project by id without owner scoping. Use carefully for signed callbacks. */
    Optional<Project> findById(Long projectId);

    /** Save or update the parent profile for a user. */
    ParentProfile saveParentProfile(User owner, String parentName, String relation,
                                    String zaloContact, String additionalPhone, String notes);

    /** Persist a new photo asset for a project. */
    PhotoAsset addPhotoAsset(Project project, String originalFilename, String storagePath,
                             String caption, String chapterTag,
                             PhotoAsset.RestorationStatus restorationStatus);

    /** Update the editable metadata for a photo asset. */
    PhotoAsset updatePhotoAsset(PhotoAsset photo, String caption, String chapterTag,
                                PhotoAsset.RestorationStatus restorationStatus);

    /** Remove a photo asset from its project. */
    void deletePhotoAsset(PhotoAsset photo);

    /** Save print handoff details on the project. */
    Project saveHandoffDetails(Project project, String vendorName, String deliveryAddress, String notes);

    /** Save cloud archive details on the project. */
    Project saveArchiveDetails(Project project, String archiveUrl, String archiveProvider, String archiveNotes);

    /** Record a confirmed payment result from the gateway. */
    Project recordPayment(Project project,
                          String paymentGateway,
                          String paymentReference,
                          String paymentTransactionNo,
                          String paymentResponseCode,
                          String paymentBankCode);

    /** Mark the project as completed after publishing handoff is finished. */
    Project completeProject(Project project);

    /** Retrieve the parent profile for a user, if it exists. */
    Optional<ParentProfile> findParentProfile(User owner);

    /**
     * Dashboard summary DTO — aggregates the data shown on the dashboard
     * without exposing lazy collections directly to Thymeleaf.
     */
    record DashboardSummary(
        Project project,
        long totalStories,
        long approvedStories,
        long totalPhotos,
        boolean parentProfileSet,
        boolean parentConnected
    ) {}

    /** Build the dashboard summary for the user's most recent project, or empty. */
    Optional<DashboardSummary> buildDashboardSummary(User owner);
}
