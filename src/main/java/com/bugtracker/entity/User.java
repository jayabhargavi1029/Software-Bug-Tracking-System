package com.bugtracker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** An account identity retained for administration and historical attribution. */
@Entity
@Table(name = "app_users")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    /** BCrypt hash - the plain password is never stored. */
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "access_revoked", nullable = false, columnDefinition = "BIT DEFAULT 0")
    private boolean accessRevoked;

    /** Marks legacy accounts whose identifying details were anonymized. */
    @Column(nullable = false)
    private boolean anonymized;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isAccessRevoked() { return accessRevoked; }
    public void setAccessRevoked(boolean accessRevoked) { this.accessRevoked = accessRevoked; }
    public boolean isAnonymized() { return anonymized; }
    public void setAnonymized(boolean anonymized) { this.anonymized = anonymized; }

    public boolean isAdmin() { return role == Role.ADMIN; }

    /** First letter of the name, used for the round avatar in the UI. */
    public String getInitial() {
        return (fullName == null || fullName.isBlank()) ? "?" : fullName.substring(0, 1).toUpperCase();
    }
}
