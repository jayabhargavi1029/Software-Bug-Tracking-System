package com.bugtracker.dto;

import com.bugtracker.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Data typed in the "Add User" form (admin only). */
public class UserForm {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3 to 50 characters")
    @Pattern(regexp = "[A-Za-z0-9._-]*", message = "Use only letters, digits, dot, dash and underscore")
    private String username;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String fullName;

    @NotBlank(message = "E-mail is required")
    @Email(message = "Enter a valid e-mail address")
    @Size(max = 120, message = "E-mail must be at most 120 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 72, message = "Password must be 6 to 72 characters")
    private String password;

    @NotNull(message = "Please choose a role")
    private Role role;

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
}
