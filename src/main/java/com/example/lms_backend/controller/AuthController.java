package com.example.lms_backend.controller;

import com.example.lms_backend.model.UserEntity;
import com.example.lms_backend.repository.UserRepo;
import com.example.lms_backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.Collections;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private NotificationService notificationService;

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

        notificationService.sendAndSaveNotification(
            saved,
            "Welcome to Aptitude Workspace!",
            "Your account has been successfully created. We're excited to have you on board!"
        );

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

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> payload) {
        String tokenString = payload.get("token");
        if (tokenString == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token missing"));
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList("299098000450-b3ni04dust2u07h898geb3lkmkcrvvc6.apps.googleusercontent.com"))
                    .build();

            GoogleIdToken idToken = verifier.verify(tokenString);
            if (idToken != null) {
                GoogleIdToken.Payload googlePayload = idToken.getPayload();
                String email = googlePayload.getEmail();
                String name = (String) googlePayload.get("name");
                String pictureUrl = (String) googlePayload.get("picture");

                Optional<UserEntity> userOpt = userRepo.findByEmail(email);
                UserEntity user;
                if (userOpt.isPresent()) {
                    user = userOpt.get();
                    if (pictureUrl != null && user.getImageUrl() == null) {
                        user.setImageUrl(pictureUrl); // update picture if missing
                        user = userRepo.save(user);
                    }
                } else {
                    user = new UserEntity();
                    user.setEmail(email);
                    user.setPassword(""); // No password for oauth users
                    user.setFirstName(name);
                    user.setImageUrl(pictureUrl);
                    user.setUserType("Student");
                    user = userRepo.save(user);

                    notificationService.sendAndSaveNotification(
                        user,
                        "Welcome to Aptitude Workspace!",
                        "Your account has been successfully created via Google. We're excited to have you on board!"
                    );
                }
                return ResponseEntity.ok(user);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid ID token."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Google auth failed: " + e.getMessage()));
        }
    }
}
