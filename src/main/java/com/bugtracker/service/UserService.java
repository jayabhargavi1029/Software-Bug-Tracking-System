package com.bugtracker.service;

import com.bugtracker.dto.UserForm;
import com.bugtracker.entity.Role;
import com.bugtracker.entity.User;
import com.bugtracker.exception.DuplicateUserException;
import com.bugtracker.exception.ResourceNotFoundException;
import com.bugtracker.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business logic for user accounts. */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User create(UserForm form) {
        return createUser(form.getUsername(), form.getFullName(), form.getEmail(), form.getPassword(), form.getRole());
    }

    /** Creates a user after checking that the username and e-mail are not already used. */
    @Transactional
    public User createUser(String username, String fullName, String email, String rawPassword, Role role) {
        String cleanUsername = username.trim().toLowerCase();
        String cleanEmail = email.trim().toLowerCase();
        if (userRepository.existsByUsername(cleanUsername)) {
            throw new DuplicateUserException("username", "This username is already taken");
        }
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new DuplicateUserException("email", "This e-mail is already registered");
        }
        User user = new User();
        user.setUsername(cleanUsername);
        user.setFullName(fullName.trim());
        user.setEmail(cleanEmail);
        user.setPassword(passwordEncoder.encode(rawPassword));   // BCrypt hash
        user.setRole(role);
        user.setActive(true);
        return userRepository.save(user);
    }

    @Transactional
    public User toggleActive(Long id, User actor) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.isAnonymized() || user.isAccessRevoked()) {
            throw new IllegalArgumentException("An account with permanently removed access cannot be reactivated.");
        }
        if (user.getId().equals(actor.getId())) {
            throw new IllegalArgumentException("You cannot deactivate your own account.");
        }
        user.setActive(!user.isActive());
        return userRepository.save(user);
    }

    @Transactional
    public User removeAccess(Long id, User actor) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getId().equals(actor.getId())) {
            throw new IllegalArgumentException("You cannot remove access to your own account.");
        }
        if (user.isAnonymized() || user.isAccessRevoked()) {
            throw new IllegalArgumentException("Login access has already been removed for this account.");
        }
        if (user.getRole() == Role.ADMIN && user.isActive()
                && userRepository.countByRoleAndActiveTrue(Role.ADMIN) <= 1) {
            throw new IllegalArgumentException("Access to the last active administrator account cannot be removed.");
        }

        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setActive(false);
        user.setAccessRevoked(true);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAllByOrderByRoleAscFullNameAsc();
    }

    @Transactional(readOnly = true)
    public List<User> activeDevelopers() {
        return userRepository.findByRoleAndActiveTrueOrderByFullNameAsc(Role.DEVELOPER);
    }
}
