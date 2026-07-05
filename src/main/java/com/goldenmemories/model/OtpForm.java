package com.goldenmemories.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class OtpForm {
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 6, max = 6)
    private String code;

    public OtpForm() {
    }

    public OtpForm(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
