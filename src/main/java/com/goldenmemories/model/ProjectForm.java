package com.goldenmemories.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProjectForm {

    @NotBlank(message = "Project title is required.")
    private String title;

    @NotNull(message = "Please select a package.")
    private Project.Package selectedPackage;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Project.Package getSelectedPackage() { return selectedPackage; }
    public void setSelectedPackage(Project.Package selectedPackage) { this.selectedPackage = selectedPackage; }
}
