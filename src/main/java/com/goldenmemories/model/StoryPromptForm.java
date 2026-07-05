package com.goldenmemories.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class StoryPromptForm {

    @NotBlank(message = "Prompt question is required.")
    private String promptQuestion;

    @NotNull(message = "Please select a life stage.")
    private StoryEntry.LifeStage lifeStage;

    public String getPromptQuestion() { return promptQuestion; }
    public void setPromptQuestion(String promptQuestion) { this.promptQuestion = promptQuestion; }

    public StoryEntry.LifeStage getLifeStage() { return lifeStage; }
    public void setLifeStage(StoryEntry.LifeStage lifeStage) { this.lifeStage = lifeStage; }
}
