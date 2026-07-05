package com.goldenmemories.service;

public interface OtpService {

    /** Generate a 6-digit code, persist it with an expiry, and email it. */
    void issueAndSend(String email);

    /**
     * Validate the submitted code for the given email.
     *
     * @return true if the code is correct, not expired, and not already used.
     */
    boolean verify(String email, String code);
}
