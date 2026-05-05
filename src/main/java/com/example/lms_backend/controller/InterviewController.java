package com.example.lms_backend.controller;

import com.example.lms_backend.model.MockInterviewSession;
import com.example.lms_backend.model.UserEntity;
import com.example.lms_backend.repository.MockInterviewSessionRepo;
import com.example.lms_backend.repository.UserRepo;
import com.example.lms_backend.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class InterviewController {

    @Autowired
    private MockInterviewSessionRepo mockInterviewSessionRepo;

    @Autowired
    private UserRepo userRepo;

    @Value("${inworld.api.key:}")
    private String inworldApiKey;

    @Autowired
    private AIService aiService;

    @GetMapping("/interview/inworld-config")
    public ResponseEntity<?> getInworldConfig() {
        // Return the API key to the frontend so it can establish 
        // the direct WebRTC/WebSocket to Inworld Realtime API
        return ResponseEntity.ok(Map.of("apiKey", inworldApiKey));
    }

    @PostMapping("/interview/chat")
    public ResponseEntity<?> interviewChat(@RequestBody Map<String, Object> req) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> messages = (List<Map<String, String>>) req.get("messages");
            String reply = aiService.generateInterviewResponse(messages);
            return ResponseEntity.ok(Map.of("reply", reply));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/interview/session/save")
    public ResponseEntity<?> saveSession(@RequestBody Map<String, Object> payload) {
        try {
            Long userId = Long.valueOf(payload.get("userId").toString());
            String targetRole = payload.get("targetRole") != null ? payload.get("targetRole").toString() : "Mock Interview";
            String conversationHistory = payload.get("conversationHistory").toString();

            Optional<UserEntity> userOpt = userRepo.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("User not found");
            }

            MockInterviewSession session;
            if (payload.containsKey("id") && payload.get("id") != null) {
                Long sessionId = Long.valueOf(payload.get("id").toString());
                session = mockInterviewSessionRepo.findById(sessionId).orElse(new MockInterviewSession());
                session.setUser(userOpt.get());
            } else {
                session = new MockInterviewSession();
                session.setUser(userOpt.get());
            }
            
            session.setTargetRole(targetRole);
            session.setConversationHistory(conversationHistory);

            MockInterviewSession saved = mockInterviewSessionRepo.save(session);
            return ResponseEntity.ok(Map.of("message", "Session saved", "id", saved.getId()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error saving session: " + e.getMessage());
        }
    }

    @GetMapping("/interview/session/user/{userId}")
    public ResponseEntity<?> getUserSessions(@PathVariable("userId") Long userId) {
        try {
            List<MockInterviewSession> historyList = mockInterviewSessionRepo.findByUserIdOrderByCreatedAtDesc(userId);
            List<Map<String, Object>> result = historyList.stream().map(h -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", h.getId());
                map.put("targetRole", h.getTargetRole());
                map.put("conversationHistory", h.getConversationHistory());
                map.put("createdAt", h.getCreatedAt().toString());
                return map;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching sessions: " + e.getMessage());
        }
    }

    @DeleteMapping("/interview/session/{id}")
    public ResponseEntity<?> deleteSession(@PathVariable("id") Long id) {
        try {
            if (!mockInterviewSessionRepo.existsById(id)) {
                return ResponseEntity.badRequest().body("Session not found");
            }
            mockInterviewSessionRepo.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Session deleted"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error deleting session: " + e.getMessage());
        }
    }
}