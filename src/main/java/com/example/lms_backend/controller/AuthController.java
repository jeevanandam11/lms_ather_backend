package com.example.lms_backend.controller;

import com.example.lms_backend.model.UserEntity;
import com.example.lms_backend.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private UserRepo userRepo;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");

        if (email == null || !email.toLowerCase().endsWith("@gmail.com")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Validation Error: Email must be a valid @gmail.com address."));
        }

        String passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$";
        if (password == null || !password.matches(passwordRegex)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Validation Error: Password must be at least 8 characters long, contain an uppercase letter, a lowercase letter, a number, and a special character."));
        }

        Optional<UserEntity> existing = userRepo.findByEmail(email);
        if (existing.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "User with this email already exists."));
        }

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPassword(password); // In a real app, hash the password (e.g. BCrypt)
        
        // Set some placeholder defaults
        user.setFirstName(email.split("@")[0]);
        user.setUserType("Student");

        UserEntity saved = userRepo.save(user);

        return ResponseEntity.ok(saved);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");

        Optional<UserEntity> userOpt = userRepo.findByEmail(email);
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            if (user.getPassword().equals(password)) {
                return ResponseEntity.ok(user);
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid email or password."));
    }
}
