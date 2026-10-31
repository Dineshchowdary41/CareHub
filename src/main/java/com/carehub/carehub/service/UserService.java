package com.carehub.carehub.service;

import com.carehub.carehub.config.ResourceNotFoundException;
import com.carehub.carehub.entity.User;
import com.carehub.carehub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public User getOne(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
    }

    public User create(User user) {
        if (userRepository.existsByUsernameIgnoreCase(user.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        if (user.getIsActive() == null) {
            user.setIsActive(true);
        }
        return userRepository.save(user);
    }

    /** Updates username/role only — password changes go through resetPassword(). */
    public User update(Long id, User updated) {
        User existing = getOne(id);
        existing.setUsername(updated.getUsername());
        existing.setRole(updated.getRole());
        if (updated.getIsActive() != null) {
            existing.setIsActive(updated.getIsActive());
        }
        return userRepository.save(existing);
    }

    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id " + id);
        }
        userRepository.deleteById(id);
    }

    public User resetPassword(Long id, String newPassword) {
        User existing = getOne(id);
        existing.setPasswordHash(passwordEncoder.encode(newPassword));
        return userRepository.save(existing);
    }

    public User changeRole(Long id, String newRole) {
        User existing = getOne(id);
        existing.setRole(newRole);
        return userRepository.save(existing);
    }

    public User setActive(Long id, boolean active) {
        User existing = getOne(id);
        existing.setIsActive(active);
        return userRepository.save(existing);
    }
}
