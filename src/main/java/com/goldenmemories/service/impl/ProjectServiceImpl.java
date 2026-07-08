package com.goldenmemories.service.impl;

import com.goldenmemories.model.ParentProfile;
import com.goldenmemories.model.PhotoAsset;
import com.goldenmemories.model.Project;
import com.goldenmemories.model.StoryEntry;
import com.goldenmemories.model.User;
import com.goldenmemories.repository.ParentProfileRepository;
import com.goldenmemories.repository.PhotoAssetRepository;
import com.goldenmemories.repository.ProjectRepository;
import com.goldenmemories.repository.StoryEntryRepository;
import com.goldenmemories.service.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final StoryEntryRepository storyEntryRepository;
    private final PhotoAssetRepository photoAssetRepository;

    public ProjectServiceImpl(ProjectRepository projectRepository,
                               ParentProfileRepository parentProfileRepository,
                               StoryEntryRepository storyEntryRepository,
                               PhotoAssetRepository photoAssetRepository) {
        this.projectRepository = projectRepository;
        this.parentProfileRepository = parentProfileRepository;
        this.storyEntryRepository = storyEntryRepository;
        this.photoAssetRepository = photoAssetRepository;
    }

    @Override
    public Project createProject(User owner, String title, Project.Package pkg) {
        Project project = new Project();
        project.setOwner(owner);
        project.setTitle(title);
        project.setSelectedPackage(pkg);
        return projectRepository.save(project);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> listProjects(User owner) {
        return projectRepository.findByOwnerOrderByCreatedAtDesc(owner);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Project> findByIdAndOwner(Long projectId, User owner) {
        return projectRepository.findById(projectId)
            .filter(p -> p.getOwner().getId().equals(owner.getId()));
    }

    @Override
    public ParentProfile saveParentProfile(User owner, String parentName, String relation,
                                            String zaloContact, String additionalPhone, String notes) {
        ParentProfile profile = parentProfileRepository.findByOwner(owner)
            .orElseGet(ParentProfile::new);
        profile.setOwner(owner);
        profile.setParentName(parentName);
        profile.setRelation(relation);
        profile.setZaloContact(zaloContact);
        profile.setAdditionalPhone(additionalPhone);
        profile.setNotes(notes);
        return parentProfileRepository.save(profile);
    }

    @Override
    public PhotoAsset addPhotoAsset(Project project,
                                    String originalFilename,
                                    String storagePath,
                                    String caption,
                                    String chapterTag,
                                    PhotoAsset.RestorationStatus restorationStatus) {
        PhotoAsset photo = new PhotoAsset();
        photo.setProject(project);
        photo.setOriginalFilename(originalFilename);
        photo.setStoragePath(storagePath);
        photo.setCaption(caption);
        photo.setChapterTag(chapterTag);
        photo.setRestorationStatus(restorationStatus);
        project.getPhotos().add(photo);
        return photoAssetRepository.save(photo);
    }

    @Override
    public PhotoAsset updatePhotoAsset(PhotoAsset photo,
                                       String caption,
                                       String chapterTag,
                                       PhotoAsset.RestorationStatus restorationStatus) {
        photo.setCaption(caption);
        photo.setChapterTag(chapterTag);
        photo.setRestorationStatus(restorationStatus);
        return photoAssetRepository.save(photo);
    }

    @Override
    public void deletePhotoAsset(PhotoAsset photo) {
        Project project = photo.getProject();
        if (project != null) {
            project.getPhotos().remove(photo);
        }
        photoAssetRepository.delete(photo);
    }

    @Override
    public Project completeProject(Project project) {
        project.setCurrentPhase(Project.Phase.COMPLETED);
        return projectRepository.save(project);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ParentProfile> findParentProfile(User owner) {
        return parentProfileRepository.findByOwner(owner);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DashboardSummary> buildDashboardSummary(User owner) {
        return projectRepository.findFirstByOwnerOrderByCreatedAtDesc(owner)
            .map(project -> {
                long totalStories   = storyEntryRepository.countByProject(project);
                long approvedStories = storyEntryRepository.countByProjectAndStatus(
                    project, StoryEntry.Status.APPROVED);
                long totalPhotos    = photoAssetRepository.countByProject(project);

                Optional<ParentProfile> profile = parentProfileRepository.findByOwner(owner);
                boolean profileSet   = profile.isPresent();
                boolean connected    = profile.map(ParentProfile::isConnected).orElse(false);

                return new DashboardSummary(project, totalStories, approvedStories,
                    totalPhotos, profileSet, connected);
            });
    }
}
