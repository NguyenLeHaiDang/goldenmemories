package com.goldenmemories.model;

import jakarta.validation.constraints.NotBlank;

public class TranscriptForm {

    @NotBlank(message = "Transcript content cannot be empty.")
    private String rawTranscript;

    public String getRawTranscript() { return rawTranscript; }
    public void setRawTranscript(String rawTranscript) { this.rawTranscript = rawTranscript; }
}
