package com.goldenmemories.model;

import jakarta.validation.constraints.NotBlank;

public class StoryEditForm {

    @NotBlank(message = "Edited content cannot be empty.")
    private String editedContent;

    private String editorNotes;

    public String getEditedContent() { return editedContent; }
    public void setEditedContent(String editedContent) { this.editedContent = editedContent; }

    public String getEditorNotes() { return editorNotes; }
    public void setEditorNotes(String editorNotes) { this.editorNotes = editorNotes; }
}
