package com.maxxenergy.auth;

// src/main/java/com/maxxenergy/user/UsersController.java

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

record UserProfileResponse(String username, String email, String role) {}
record UpdateProfileRequest(String email) {}

@RestController
@RequestMapping("/api/users")
public class UsersController {
    private final UserRepository repo;

    public UsersController(UserRepository repo) {
        this.repo = repo;
    }

    // Current user profile
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        var u = repo.findByUsername(auth.getName()).orElseThrow();
        return ResponseEntity.ok(new UserProfileResponse(u.getUsername(), u.getEmail(), u.getRole()));
    }

    // DEV-ONLY: expose password hash (remove/disable in prod)
    @GetMapping("/me/hash")
    public ResponseEntity<?> myHash(Authentication auth) {
        var u = repo.findByUsername(auth.getName()).orElseThrow();
        return ResponseEntity.ok(java.util.Map.of("hash", u.getPassword()));
    }

    // Update editable fields
    @PutMapping("/me")
    public ResponseEntity<?> updateMe(Authentication auth, @RequestBody UpdateProfileRequest req) {
        var u = repo.findByUsername(auth.getName()).orElseThrow();
        if (req.email() != null && !req.email().isBlank()) u.setEmail(req.email());
        repo.save(u);
        return ResponseEntity.ok(new UserProfileResponse(u.getUsername(), u.getEmail(), u.getRole()));
    }
}
