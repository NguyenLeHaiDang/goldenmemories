package com.goldenmemories.service.impl;

import com.goldenmemories.model.OtpToken;
import com.goldenmemories.repository.OtpTokenRepository;
import com.goldenmemories.service.OtpService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class EmailOtpService implements OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpTokenRepository otpTokenRepository;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@goldenmemories.local}")
    private String fromAddress;

    @Value("${app.otp.expiry-minutes:10}")
    private int expiryMinutes;

    public EmailOtpService(OtpTokenRepository otpTokenRepository, JavaMailSender mailSender) {
        this.otpTokenRepository = otpTokenRepository;
        this.mailSender = mailSender;
    }

    @Override
    @Transactional
    public void issueAndSend(String email) {
        // Invalidate any existing tokens for this email before issuing a new one
        otpTokenRepository.invalidateAll(email);

        String code = generateCode();
        Instant expiresAt = Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES);
        otpTokenRepository.save(new OtpToken(email, code, expiresAt));

        sendEmail(email, code);
    }

    @Override
    @Transactional
    public boolean verify(String email, String code) {
        return otpTokenRepository.findActiveToken(email)
            .filter(token -> token.getCode().equals(code))
            .map(token -> {
                token.setUsed(true);
                otpTokenRepository.save(token);
                return true;
            })
            .orElse(false);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private String generateCode() {
        int code = 100000 + RANDOM.nextInt(900000);
        return Integer.toString(code);
    }

    private void sendEmail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("Your Ký Ức Vàng verification code");
        message.setText("""
            Your one-time verification code is:

                %s

            This code expires in %d minutes. If you did not request it, you can ignore this email.
            """.formatted(code, expiryMinutes));
        mailSender.send(message);
    }
}
