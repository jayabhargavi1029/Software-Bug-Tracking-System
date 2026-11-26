package com.bugtracker.repository;

import com.bugtracker.entity.Role;
import com.bugtracker.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    long countByRoleAndActiveTrue(Role role);
    List<User> findByRoleAndActiveTrueOrderByFullNameAsc(Role role);
    List<User> findAllByOrderByRoleAscFullNameAsc();
}
