package com.example.lms_backend.controller;

import com.example.lms_backend.model.TopicHistory;
import com.example.lms_backend.model.UserEntity;
import com.example.lms_backend.repository.TopicHistoryRepository;
import com.example.lms_backend.repository.UserRepo;
import com.example.lms_backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = "*")
public class TopicHistoryController {

    @Autowired
    private TopicHistoryRepository topicHistoryRepository;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private NotificationService notificationService;

    @PostMapping("/save")
    public ResponseEntity<?> saveHistory(@RequestBody Map<String, Object> payload) {
        try {
            Long userId = Long.valueOf(payload.get("userId").toString());
            String topicName = payload.get("topicName").toString();
            String pdfBase64 = payload.get("pdfBase64") != null ? payload.get("pdfBase64").toString() : "";
            String lessonData = payload.get("lessonData") != null ? payload.get("lessonData").toString() : null;

            Optional<UserEntity> userOpt = userRepo.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("User not found");
            }

            TopicHistory history = new TopicHistory();
            history.setUser(userOpt.get());
            history.setTopicName(topicName);
            history.setPdfBase64(pdfBase64);
            history.setLessonData(lessonData);

            TopicHistory saved = topicHistoryRepository.save(history);

            notificationService.sendAndSaveNotification(
                userOpt.get(),
                "New Course Generated",
                "Your interactive course on '" + topicName + "' has been successfully generated and saved to your library."
            );

            return ResponseEntity.ok(Map.of("message", "History saved", "id", saved.getId()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error saving history: " + e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserHistory(@PathVariable("userId") Long userId) {
        try {
            List<TopicHistory> historyList = topicHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
            List<Map<String, Object>> result = historyList.stream().map(h -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", h.getId());
                map.put("topicName", h.getTopicName());
                map.put("pdfBase64", h.getPdfBase64());
                map.put("createdAt", h.getCreatedAt().toString());
                return map;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching history: " + e.getMessage());
        }
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<?> getHistoryById(@PathVariable("id") Long id) {
        try {
            Optional<TopicHistory> historyOpt = topicHistoryRepository.findById(id);
            if (historyOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("History not found");
            }
            TopicHistory h = historyOpt.get();
            Map<String, Object> map = new HashMap<>();
            map.put("id", h.getId());
            map.put("topicName", h.getTopicName());
            map.put("pdfBase64", h.getPdfBase64());
            map.put("lessonData", h.getLessonData());
            map.put("createdAt", h.getCreatedAt().toString());
            
            return ResponseEntity.ok(map);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching history details: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteHistory(@PathVariable("id") Long id) {
        try {
            if (!topicHistoryRepository.existsById(id)) {
                return ResponseEntity.badRequest().body("History not found");
            }
            topicHistoryRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "History deleted"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error deleting history: " + e.getMessage());
        }
    }
}
