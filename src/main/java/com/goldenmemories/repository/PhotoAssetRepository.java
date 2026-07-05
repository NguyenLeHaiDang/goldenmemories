package com.goldenmemories.repository;

import com.goldenmemories.model.PhotoAsset;
import com.goldenmemories.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhotoAssetRepository extends JpaRepository<PhotoAsset, Long> {

    List<PhotoAsset> findByProjectOrderByUploadedAtDesc(Project project);

    long countByProject(Project project);
}
