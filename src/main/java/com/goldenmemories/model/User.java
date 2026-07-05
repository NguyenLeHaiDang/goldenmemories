package com.goldenmemories.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Core account identity.  Supports both local (email + password) sign-up and
 * social OAuth2 logins (Facebook, Google).
 */
@Entity
@Table(name = "app_user")
public class User {

    public enum LoginMethod { LOCAL, FACEBOOK, GOOGLE }
    public enum Role { USER, ADMIN }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    /** Bcrypt-hashed password – null for OAuth2-only accounts. */
    private String passwordHash;

    @Column(nullable = false)
    private String fullName;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginMethod loginMethod = LoginMethod.LOCAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    /** True once email OTP has been verified (or after OAuth2 login). */
    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    // ── Getters & setters ────────────────────────────────────────────────────

    public Long getId() { return id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public LoginMethod getLoginMethod() { return loginMethod; }
    public void setLoginMethod(LoginMethod loginMethod) { this.loginMethod = loginMethod; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public Instant getCreatedAt() { return createdAt; }
}
