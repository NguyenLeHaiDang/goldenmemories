package com.goldenmemories.service;

import com.goldenmemories.model.ParentProfile;
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

    /** Save or update the parent profile for a user. */
    ParentProfile saveParentProfile(User owner, String parentName, String relation,
                                    String zaloContact, String additionalPhone, String notes);

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
