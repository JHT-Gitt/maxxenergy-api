// package stays the same as your current file
package com.maxxenergy.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

record UserProfileResponse(String username, String email, String role) {}
record UpdateProfileRequest(String email) {}
record ChangePasswordRequest(String currentPassword, String newPassword) {}

@RestController
@RequestMapping("/api/users")
public class UsersController {
    private final UserRepository repo;
    private final PasswordEncoder encoder;   // <-- add

    public UsersController(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        var u = repo.findByUsername(auth.getName()).orElseThrow();
        return ResponseEntity.ok(new UserProfileResponse(u.getUsername(), u.getEmail(), u.getRole()));
    }

    // DEV-ONLY: remove in prod
    @GetMapping("/me/hash")
    public ResponseEntity<?> myHash(Authentication auth) {
        var u = repo.findByUsername(auth.getName()).orElseThrow();
        return ResponseEntity.ok(java.util.Map.of("hash", u.getPassword()));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(Authentication auth, @RequestBody UpdateProfileRequest req) {
        var u = repo.findByUsername(auth.getName()).orElseThrow();
        if (req.email() != null && !req.email().isBlank()) u.setEmail(req.email());
        repo.save(u);
        return ResponseEntity.ok(new UserProfileResponse(u.getUsername(), u.getEmail(), u.getRole()));
    }

    // NEW: change password
    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(Authentication auth, @RequestBody ChangePasswordRequest req) {
        if (req.currentPassword() == null || req.newPassword() == null) {
            return ResponseEntity.badRequest().body("Missing password fields");
        }
        if (req.newPassword().length() < 8) {
            return ResponseEntity.badRequest().body("New password must be at least 8 characters");
        }

        var u = repo.findByUsername(auth.getName()).orElseThrow();
        if (!encoder.matches(req.currentPassword(), u.getPassword())) {
            return ResponseEntity.badRequest().body("Current password is incorrect");
        }

        u.setPassword(encoder.encode(req.newPassword()));
        repo.save(u);
        return ResponseEntity.ok().build();
    }
}
