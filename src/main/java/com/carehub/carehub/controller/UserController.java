package com.carehub.carehub.controller;

import com.carehub.carehub.entity.User;
import com.carehub.carehub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public List<User> getAll() {
        return userService.getAll();
    }

    @GetMapping("/{id}")
    public User getOne(@PathVariable Long id) {
        return userService.getOne(id);
    }

    @PostMapping
    public User create(@Valid @RequestBody User user) {
        return userService.create(user);
    }

    @PutMapping("/{id}")
    public User update(@PathVariable Long id, @RequestBody User updated) {
        return userService.update(id, updated);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }

    @PatchMapping("/{id}/reset-password")
    public User resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return userService.resetPassword(id, body.get("newPassword"));
    }

    @PatchMapping("/{id}/role")
    public User changeRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return userService.changeRole(id, body.get("role"));
    }

    @PatchMapping("/{id}/activate")
    public User activate(@PathVariable Long id) {
        return userService.setActive(id, true);
    }

    @PatchMapping("/{id}/deactivate")
    public User deactivate(@PathVariable Long id) {
        return userService.setActive(id, false);
    }
}
